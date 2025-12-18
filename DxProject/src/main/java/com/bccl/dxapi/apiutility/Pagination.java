package com.bccl.dxapi.apiutility;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import com.bccl.dxapi.controller.InternalportalController;

public class Pagination {

	static Logger log = Logger.getLogger(Pagination.class.getName());

	String strQuery;
	ResultSet rs = null;
	PreparedStatement ps = null;
	String originalquery;
	public static int rowsperpage = 50;

	public int getPages(Connection con, ArrayList<String> param) throws SQLException {
		String q = "select count(*) from ( " + originalquery + ") a";
		ps = con.prepareStatement(q);
		for (int i = 0; i < param.size(); i++) {
			ps.setString(i + 1, param.get(i));
		}
		rs = ps.executeQuery();
		int count = 0;
		if (rs.next()) {
			count = rs.getInt(1);
		}
		return (int) Math.ceil(count);
	}

	public Pagination(String sQuery, int nPage) {

		if (nPage == 0) {
			strQuery = "select * from ( select a.*, rownum rnum from ( " + sQuery + " ) a )";
		} else {

			strQuery = "select * from ( select a.*, rownum rnum from ( " + sQuery + " ) a  where rownum <= "
					+ (nPage * rowsperpage) + ") where rnum >= " + (nPage * rowsperpage - rowsperpage + 1);
		}
		originalquery = sQuery;
	}

	public Pagination(String sQuery, int nPage, int version) {

		if (nPage == 0) {
			strQuery = sQuery;
		} else {

			strQuery = sQuery.substring(0, sQuery.length() - 1) + " where rownum <= " + (nPage * rowsperpage)
					+ ") where rnum >= " + (nPage * rowsperpage - rowsperpage + 1);
		}
		originalquery = sQuery;
	}

	public ResultSet execute(Connection con, ArrayList<String> param) throws SQLException {

		ps = con.prepareStatement(strQuery);

		for (int i = 0; i < param.size(); i++) {

			ps.setString(i + 1, param.get(i));

		}

		return ps.executeQuery();
	}

	public void close() throws SQLException {
		if (rs != null)
			rs.close();
		if (ps != null)
			ps.close();
		rs = null;
		ps = null;
	}

}
