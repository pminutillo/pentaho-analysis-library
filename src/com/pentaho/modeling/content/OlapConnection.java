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

package com.pentaho.modeling.content;

import java.sql.SQLException;

import mondrian.xmla.XmlaHandler.XmlaExtra;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.olap4j.OlapException;
import org.pentaho.platform.plugin.action.olap.PlatformXmlaExtra;

/**
 * OlapConnection contains a role-specific Mondrian connection. The connection may use a dynamic rolap schema which may
 * have been localized or acesss control modified which is why the OlapConnection cacheKey needs to include the
 * RolapSchema ID.
 * 
 * The cacheKey is used to lookup Analyzer's HelpGenerator cache which can be reused across different Mondrian
 * connections who share the same roles and RolapSchema.
 * 
 * @author bchow
 * @version $Id: $
 * @created Sep 6, 2009
 * @updated $DateTime: $
 */
public class OlapConnection implements java.io.Serializable {

  private static final long serialVersionUID = -5695053103477008156L; /* EESOURCE: UPDATE SERIALVERUID */
  String catalog;
  private static final Log logger = LogFactory.getLog( OlapConnection.class );
  private String cacheKey;
  private org.olap4j.OlapConnection connection;
  private boolean isMondrian = true;
  private int dbVersion;

  public static final String CUSTOM_DATASOURCE_NAME = "custom.datasource.name";
  public static final String CUSTOM_DATASOURCE_URL = "custom.datasource.url";
  public static final String CUSTOM_DATASOURCE_DRIVER = "custom.datasource.driver";

  public OlapConnection( String catalog, String url, org.olap4j.OlapConnection connection ) {
    super();
    this.catalog = catalog;
    this.connection = connection;
    StringBuffer buffer = new StringBuffer();
    buffer.append( "ANALYZER~" );
    buffer.append( catalog );
    try {
      url = connection.getMetaData().getURL();
    } catch ( Exception e ) {
      throw new RuntimeException( e );
    }
    buffer.append( "~" ).append( url );

    // For mondrian, we need to also append the RolapSchema.Id in case a dynamic schema processor is being used. See
    // ANALYZER-362
    try {
      XmlaExtra xmlaExtra = getXmlaExtra();
      if ( xmlaExtra != null ) {
        buffer.append( "~" ).append( xmlaExtra.getSchemaId( connection.getOlapSchema() ) );
      }
    } catch ( OlapException e ) {
      throw new RuntimeException( e );
    }

    try {
      String dbProductName = connection.getMetaData().getDatabaseProductName();
      dbVersion = connection.getMetaData().getDatabaseMajorVersion();
      if ( !"mondrian".equals( dbProductName ) ) {
        isMondrian = false;
      }
    } catch ( SQLException e ) {
      logger.debug( e.getMessage(), e );
    }

    cacheKey = buffer.toString();
  }

  /**
   * Returns the cache key for this OlapConnection. The cache key consists of the catalog,
   * connection URL (which may
   * contain roles) and the schemaId (applicable to dynamic schemas). Two different users who have the same roles will
   * have the same OlapConnection cache key and hence share their HelpGenerator cache (i.e. access controlled cubes and
   * fields).
   * 
   * Example: DataSource=Foodmart; DynamicSchemaProcessor=mondrian.i18n.LocalizingDynamicSchemaProcessor;
   * UseContentChecksum=true; Locale=en_US; Catalog=solution:steel-wheels/analysis/foodmart.mondrian.xml;
   * Role=California manager
   * 
   * @return
   */
  public String getKey() {
    return cacheKey;
  }

  public String getCatalog() {
    return catalog;
  }

  public org.olap4j.OlapConnection getConnection() {
    return connection;
  }

  /**
   * If the underlying olap4j provider is Mondrian we get some very nice enhanced functionality like drill through
   * counts and annotations. If not, then Analyzer will take some default actions that works across all OLAP providers.
   * 
   * @return
   */
  public XmlaExtra getXmlaExtra() {
    try {
      return PlatformXmlaExtra.unwrapXmlaExtra( getConnection() );
    } catch ( SQLException e ) {
      logger.debug( e.getMessage(), e );
    }
    return null;
  }

  public void close() {
    try {
      if ( connection != null && !connection.isClosed() ) {
        connection.close();
      }
    } catch ( SQLException e ) {
      throw new RuntimeException( "Error occurred while closing connection with cacheKey: " + cacheKey, e );
    }
  }

  public boolean isMondrian() {
    return isMondrian;
  }

  public int getDbVersion() {
    return dbVersion;
  }
}
