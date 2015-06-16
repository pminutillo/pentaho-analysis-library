/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2014 Pentaho Corporation (Pentaho). All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Pentaho and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Pentaho and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Pentaho is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Pentaho,
 * explicitly covering such access.
 */

package com.pentaho.modeling.service.impl;

import java.util.Properties;

import mondrian.olap.Util;
import mondrian.rolap.RolapConnectionProperties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.commons.connection.IPentahoConnection;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.services.connection.PentahoConnectionFactory;
import org.pentaho.platform.plugin.action.mondrian.catalog.IMondrianCatalogService;
import org.pentaho.platform.plugin.action.mondrian.catalog.MondrianCatalog;
import org.pentaho.platform.plugin.services.connections.mondrian.MDXOlap4jConnection;
import org.pentaho.platform.plugin.services.importexport.legacy.MondrianCatalogRepositoryHelper;
import org.pentaho.platform.plugin.services.importexport.legacy.MondrianCatalogRepositoryHelper.Olap4jServerInfo;
import org.pentaho.platform.util.messages.LocaleHelper;

import com.pentaho.modeling.content.OlapConnection;
import com.pentaho.modeling.service.MissingCubeMetadataException;
import com.pentaho.modeling.service.OlapConnectionManager;

/**
 * OlapConnectionManagerImpl manages connections to OLAP servers. Connections information is read from the
 * MondrainCatalogHelper and connections are actually established using olap4j backed by Mondrian.
 *
 * An olap4j connection is thread safe. Multiple queries can be run on the same connection in parallel.
 *
 * A Pentaho solution stores a list of all catalogs in solution/system/data/olap/datasources.xml
 *
 * @author bchow
 * @created Aug 20, 2006
 * @updated $DateTime: 2009/05/18 10:15:48 $
 */
public class OlapConnectionManagerImpl extends AbstractOlapConnectionManager implements OlapConnectionManager,
    java.io.Serializable {

  private static final long serialVersionUID = -8468450051066144998L; /* EESOURCE: UPDATE SERIALVERUID */

  private static final Log log = LogFactory.getLog( OlapConnectionManagerImpl.class );

  private IMondrianCatalogService mondrianCatalogService;

  protected OlapConnection createConnection( String catalogName ) {
    return createConnection( catalogName, null );
  }

  /**
   * Opens an olap4j Mondrian connection and returns the OlapConnection wrapper.
   * 
   * @param catalogName
   * @param schema - Optional Mondrina schema XML to use instead of the catalog found in the MondrianCatalogHelper
   * @return
   */
  protected OlapConnection createConnection( String catalogName, String schema ) {
    if ( mondrianCatalogService == null ) {
      mondrianCatalogService =
          PentahoSystem.get( IMondrianCatalogService.class,
              "IMondrianCatalogService", PentahoSessionHolder.getSession() ); //$NON-NLS-1$
    }

    String url = null;
    Properties properties = new Properties();
    MondrianCatalog catalog = mondrianCatalogService.getCatalog( catalogName, PentahoSessionHolder.getSession() );
    if ( catalog != null ) {
      // Construct the olap4j url for a direct mondrian backend
      Util.PropertyList connectProperties = Util.parseConnectString( catalog.getDataSourceInfo() );
      connectProperties.put( RolapConnectionProperties.Locale.name(), LocaleHelper.getLocale().toString() );
      if ( schema == null ) {
        connectProperties.put( RolapConnectionProperties.Catalog.name(), catalog.getDefinition() );
      } else {
        connectProperties.put( RolapConnectionProperties.CatalogContent.name(), schema );
      }
      connectProperties.remove( RolapConnectionProperties.Provider.name() );
      url = "jdbc:mondrian:" + connectProperties.toString();
      properties.setProperty( "url", url );
      properties.setProperty( "driver", "mondrian.olap4j.MondrianOlap4jDriver" );

    }

    if ( url == null ) {
      throw new MissingCubeMetadataException( "Unable to find catalog: " + catalogName );
    }

    return getConnectionWithProperties( properties );
  }

  /**
   * get MDX OLAP Connection using specified properties
   *
   * @param properties
   * @return
   */
  private OlapConnection getConnectionWithProperties( Properties properties ) {
    MDXOlap4jConnection connection =
      (MDXOlap4jConnection) PentahoConnectionFactory.getConnection( IPentahoConnection.MDX_OLAP4J_DATASOURCE,
        properties, PentahoSessionHolder.getSession(), null );

    if ( connection == null || !connection.initialized() ) {
      throw new RuntimeException( "Unable to get connnection " + properties.getProperty( "url" ) );
    }

    OlapConnection oc = new OlapConnection(
      properties.getProperty( RolapConnectionProperties.Catalog.name() ),
      properties.getProperty( "url" ),
      connection.getConnection() );

    log.info( "Opened olap4j connection with user="
      + PentahoSessionHolder.getSession().getName()
      + ",url=" + properties.getProperty( "url" )
      + ",cacheKey=" + oc.getKey() );

    return oc;
  }

  public OlapConnection getConnection( String catalogName, String schema ) {
    return createConnection( catalogName, schema );
  }

  public OlapConnection getConnection( String catalogName, String datasourceName, String datasourceURL, String datasourceDriver ){
    // Try to make a connection using the url and driver specified in analyzer.properties
    // This mechanism allows Analyzer to create Mondrian connections without the use of
    // MondrianCatalogHelper

    Properties properties = new Properties();
    if ( catalogName.equals( datasourceName ) ) {
      properties.setProperty( "url", datasourceURL );
      properties.setProperty( "driver", datasourceDriver );
      log.info( "Opening custom datasource with name=" + catalogName + ",driver=" + datasourceDriver + ",url=" + datasourceURL );

    } else {
      // Check the MondrianCatalogRepositoryHelper
      final IUnifiedRepository repo = PentahoSystem.get( IUnifiedRepository.class, null );
      if ( repo != null ) {
        final MondrianCatalogRepositoryHelper helper = new MondrianCatalogRepositoryHelper( repo );
        if ( helper.getOlap4jServers().contains( catalogName ) ) {
          final Olap4jServerInfo si = helper.getOlap4jServerInfo( catalogName );
          datasourceURL = si.URL;
          properties.setProperty( "url", datasourceURL );
          properties.setProperty( "driver", si.className );
          if ( si.user != null ) {
            properties.setProperty( "user", si.user );
          }
          if ( si.password != null ) {
            properties.setProperty( "password", si.password );
          }
          log.info(
            "Opening olap4j datasource with name=" + catalogName
              + ",driver=" + si.className
              + ",url=" + datasourceURL );
        }
      }
    }

    return getConnectionWithProperties( properties );
  }

//
//  /**
//   * Returns a new OlapConnection which will always open a new Olap4j connection. The Olap4j connection may
//   * internally share the same RolapSchema.
//   *
//   * @param catalogName
//   * @return
//   */
//  public OlapConnection createOlapConnection( String catalogName ) {
//    return olapMetadataManager.getConnection( catalogName );
//  }
//
//  /**
//   * Returns a new OlapConnection based on a model that been modified by annotations.
//   *
//   * @param catalogName
//   * @param annotations
//   * @return
//   */
//  public OlapConnection createOlapConnection( String catalogName, ModelAnnotationGroup annotations ) {
//    return olapMetadataManager.getConnection( catalogName, annotations );
//  }

}
