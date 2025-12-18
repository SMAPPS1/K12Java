package com.bccl.dxapi.apiutility;

import java.io.InputStream;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import org.apache.log4j.Logger;

public class Datasource {
	
	static Logger log = Logger.getLogger(Datasource.class.getName());
	static DataSource ds = null;
	
	public static DataSource getDataSource()  {

		Context cntx = null;
		try {
			
			if (ds == null) {
				InputStream input = Datasource.class.getResourceAsStream("/dxproperties.properties");
				Properties prop = new Properties();
				prop.load(input);
				cntx = new InitialContext();
				ds = (DataSource) cntx.lookup(prop.getProperty("datasource"));
				} 
					
		} catch (Exception e) {
				log.error("Exception in getDataSource :" + e.fillInStackTrace());
		}
		return ds;
	}
}