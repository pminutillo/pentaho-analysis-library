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
import org.pentaho.platform.plugin.services.connections.mondrian.MDXOlap4jConnection;
import org.pentaho.platform.util.messages.LocaleHelper;

import com.pentaho.modeling.content.OlapConnection;
import com.pentaho.modeling.service.OlapConnectionManager;

/**
 * This class manages connections within the Agile BI environment
 * 
 * @author Will Gorman (wgorman@pentaho.com)
 * @version $Id: $
 * @created Nov 13, 2009
 * @updated $DateTime: $
 */
public class AgileBIOlapConnectionManager extends AbstractOlapConnectionManager implements OlapConnectionManager,
    java.io.Serializable {

  private static final long serialVersionUID = 5997028061114527815L; /* EESOURCE: UPDATE SERIALVERUID */

  private static Log log = LogFactory.getLog( AgileBIOlapConnectionManager.class );

  /**
   * Directly uses MDXOlap4jConnection to get a Mondrian connection.
   * 
   * @param catalogName
   * @return
   */
  protected OlapConnection createConnection( String catalogName ) {

    /* AGILE BI: 1) CHECK FOR TOMCAT */

    /*
     * BENNY: BEFORE CONVERSION TO OLAP4J Properties properties = new Properties(); properties.put("Catalog", "mtm:" +
     * catalogName); //$NON-NLS-1$ //$NON-NLS-2$ properties.put("Provider", "mondrian"); //$NON-NLS-1$ //$NON-NLS-2$
     * properties.put("PoolNeeded", "false"); //$NON-NLS-1$ //$NON-NLS-2$ properties.put("dataSource", catalogName);
     * //$NON-NLS-1$
     * 
     * Connection connection = null; log.debug(ls.getString("OpenMondrianConnection", properties.toString()));
     * MDXConnection mdxConnection = new MDXConnection(); mdxConnection.setProperties(properties); connection =
     * mdxConnection.getConnection();
     */

    // BENNY: AFTER CONVERSION TO OLAP4J
    // Construct the olap4j connection string to a Mondrian specific backend
    Util.PropertyList connectProperties = new Util.PropertyList();
    connectProperties.put( RolapConnectionProperties.Catalog.name(), "mtm:" + catalogName );
    connectProperties.put( RolapConnectionProperties.Locale.name(), LocaleHelper.getLocale().toString() );
    connectProperties.put( RolapConnectionProperties.PoolNeeded.name(), "false" );
    connectProperties.put( RolapConnectionProperties.DataSource.name(), catalogName );

    String url = "jdbc:mondrian:" + connectProperties.toString();
    MDXOlap4jConnection mdxConnection = new MDXOlap4jConnection();
    Properties properties = new Properties();
    properties.setProperty( "url", url );
    properties.setProperty( "driver", "mondrian.olap4j.MondrianOlap4jDriver" );
    mdxConnection.setProperties( properties );
    OlapConnection oc = new OlapConnection( catalogName, url, mdxConnection.getConnection() );

    return oc;
  }

  public OlapConnection getConnection( String catalogName, String schema ) {
    // Not implemented
    throw new IllegalStateException();
  }

}
