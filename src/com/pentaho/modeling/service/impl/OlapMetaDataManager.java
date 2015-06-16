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

import com.pentaho.modeling.AnnotationResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.platform.api.engine.ICacheManager;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.action.mondrian.catalog.MondrianCatalogHelper;

import com.pentaho.modeling.content.OlapConnection;
import com.pentaho.modeling.service.IModelingServiceFacade;
import com.pentaho.modeling.service.OlapConnectionManager;

/**
 * OlapMetDataManager provides access to OLAP metadata including the cubes and fields the user has access to. The
 * metadata may be cached in the platform ICacheManager. The cache is located in the same region as the platform's
 * Mondrian catalog cache (mondrian-catalog-cache). If the mondrian-catalog-cache region is flushed, so is Analyzer's
 * OLAP metadata cache.
 * 
 * @author bchow
 * 
 */
public class OlapMetaDataManager implements java.io.Serializable {

  private static final long serialVersionUID = 4428170848920321320L; /* EESOURCE: UPDATE SERIALVERUID */
  private static Log log = LogFactory.getLog( OlapMetaDataManager.class );

  OlapConnectionManager olapConnectionManager;
  IModelingServiceFacade modelingService;

  public OlapMetaDataManager() {
    super();
  }

  public void setOlapConnectionManager( OlapConnectionManager olapConnectionManager ) {
    this.olapConnectionManager = olapConnectionManager;
  }

  public OlapConnection getConnection( String catalogName ) {
    return olapConnectionManager.getConnection( catalogName );
  }

  public OlapConnection getConnection( String catalogName, String schema ) {
    return olapConnectionManager.getConnection( catalogName, schema );
  }

  public OlapConnection getConnection( String catalogName, ModelAnnotationGroup annotations ) {
    if ( annotations == null ) {
      return getConnection( catalogName );
    } else {
      // Use modeling service to apply annotations to an existing datasource's model

      AnnotationResult result = modelingService.applyAnnotations( catalogName, annotations );
      String schemaXml = result.getSchema();
      return olapConnectionManager.getConnection( catalogName, schemaXml );
    }
  }

  /**
   * Clears the Mondrian schema file cache, aggregation cache, members cache, presentation admin cache, incompatible
   * rules cache.
   * 
   * @param oc
   */
  public void clearCache( OlapConnection oc ) {
    olapConnectionManager.clearMondrianCache( oc.getCatalog() );
    ICacheManager cacheMgr = PentahoSystem.getCacheManager( PentahoSessionHolder.getSession() );
    cacheMgr.removeFromRegionCache( MondrianCatalogHelper.MONDRIAN_CATALOG_CACHE_REGION, oc.getKey() );
  }

  public void setModelingService( IModelingServiceFacade modelingService ) {
    this.modelingService = modelingService;
  }

}
