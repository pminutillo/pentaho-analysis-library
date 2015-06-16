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

import mondrian.xmla.XmlaHandler.XmlaExtra;

import org.olap4j.OlapException;
import org.pentaho.platform.api.engine.ILogger;
import org.pentaho.platform.util.logging.SimpleLogger;

import com.pentaho.modeling.content.OlapConnection;

/**
 * This is an abstract class used by implementing connection managers
 * 
 * @author Will Gorman (wgorman@pentaho.com)
 * @version $Id: $
 * @created Nov 17, 2009
 * @updated $DateTime: $
 */
public abstract class AbstractOlapConnectionManager {

  private static ILogger log = new SimpleLogger( AbstractOlapConnectionManager.class.getName() );

  /**
   * @return Connection
   */
  public OlapConnection getConnection( String catalogName ) {

    return createConnection( catalogName );
  }

  public void clearMondrianCache( String catalogName ) {
    OlapConnection connection = createConnection( catalogName );

    // Closing a Mondrian connection currently flushes all cache
    try {
      XmlaExtra xmlaExtra = connection.getXmlaExtra();
      if ( xmlaExtra != null ) {
        xmlaExtra.flushSchemaCache( connection.getConnection() );
      }
    } catch ( OlapException e ) {
      throw new RuntimeException( e );
    }

    connection.close();
  }

  /**
   * Gets a native Mondrian RolapConnection. The connect string (including the catalog and datasource) is the same as
   * what JPivot/AnalysisViews uses so this results in Analyzer and JPivot sharing the same RolapSchema/cache.
   * 
   * @param catalogName
   * @return
   */
  protected abstract OlapConnection createConnection( String catalogName );

}
