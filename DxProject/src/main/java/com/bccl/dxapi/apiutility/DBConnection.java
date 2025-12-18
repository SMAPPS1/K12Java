package com.bccl.dxapi.apiutility;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.log4j.Logger;

public class DBConnection {

	static Logger log = Logger.getLogger(DBConnection.class.getName());
	
	public static Connection getConnection() throws SQLException {

		DataSource ds = null;
		try {
			ds =Datasource.getDataSource();
		} catch (Exception e) {
			log.error("Exception in getConnection :" + e.fillInStackTrace());			
		}
		return ds.getConnection();
	}

	public static void closeConnection(ResultSet rs, PreparedStatement pst, Connection conn) {

		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				log.error("Exception in closeConnection :" + e.fillInStackTrace());
			}
			rs = null;
		}
		if (pst != null) {
			try {
				pst.close();
			} catch (SQLException e) {
				log.error("Exception in closeConnection :" + e.fillInStackTrace());
			}
			pst = null;
		}
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				log.error("Exception in closeConnection :" + e.fillInStackTrace());
			}
			conn = null;
		}
	}

	public String toString() {
		return "";
	}

}
