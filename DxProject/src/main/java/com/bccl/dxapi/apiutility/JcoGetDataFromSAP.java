package com.bccl.dxapi.apiutility;

import java.util.Properties;

import org.apache.log4j.Logger;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoField;
import com.sap.conn.jco.JCoStructure;
import com.sap.conn.jco.JCoRepository;
import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.bccl.dxapi.controller.InternalportalController;
import com.sap.conn.jco.AbapException;
import com.sap.conn.jco.JCoTable;
import java.util.ArrayList;
import java.util.Hashtable;

public class JcoGetDataFromSAP {
	
	private String application = "";
	static Logger log = Logger.getLogger(JcoGetDataFromSAP.class.getName());
	// Default Constructor

	private JcoGetDataFromSAP() {

	}

	// Parameterised Constructor

	public JcoGetDataFromSAP(String appl) {
		if (appl != null && !appl.equals("")) {
			application = appl;
		}
	}

	public class MyDestinationDataProvider implements DestinationDataProvider {
		private DestinationDataEventListener eL;

		private Properties HFrank_properties;

		public Properties getDestinationProperties(String destinationName) {
			if (destinationName.equals("HFrank") && HFrank_properties != null)
				return HFrank_properties;
			return null;
		}

		public void setDestinationDataEventListener(DestinationDataEventListener eventListener) {
			this.eL = eventListener;
		}

		public boolean supportsEvents() {
			return true;
		}

		void changePropertiesForABAP_AS(Properties properties) {
			if (properties == null) {
				HFrank_properties = null;
				eL.deleted("HFrank");
			} else {
				if (HFrank_properties == null || !HFrank_properties.equals(properties)) {
					HFrank_properties = properties;
					eL.updated("HFrank");
				}
			}
		}
	}

	public Hashtable jcoGetData(Hashtable SAPConnectionDetails, Hashtable SAPColumnHeads, Hashtable SAPValues)
			throws Exception {

		String ASHOST = (String) SAPConnectionDetails.get("HOSTNAME");
		String SYSNR = (String) SAPConnectionDetails.get("SYSTEMNO");
		String CLIENT = (String) SAPConnectionDetails.get("CLIENT");
		String USER = (String) SAPConnectionDetails.get("USERID");
		String PASSWD = (String) SAPConnectionDetails.get("PASSWORD");
		String LANG = (String) SAPConnectionDetails.get("LANGUAGE");

		String RFCName = (String) SAPConnectionDetails.get("RFCNAME");

		Properties connectProperties = new Properties();

		connectProperties.setProperty(DestinationDataProvider.JCO_ASHOST, ASHOST);
		connectProperties.setProperty(DestinationDataProvider.JCO_SYSNR, SYSNR);
		connectProperties.setProperty(DestinationDataProvider.JCO_CLIENT, CLIENT);
		connectProperties.setProperty(DestinationDataProvider.JCO_USER, USER);
		connectProperties.setProperty(DestinationDataProvider.JCO_PASSWD, PASSWD);
		connectProperties.setProperty(DestinationDataProvider.JCO_LANG, LANG);

		MyDestinationDataProvider myProvider = new MyDestinationDataProvider();

		if (!com.sap.conn.jco.ext.Environment.isDestinationDataProviderRegistered()) {
			com.sap.conn.jco.ext.Environment.registerDestinationDataProvider(myProvider);
			myProvider.changePropertiesForABAP_AS(connectProperties);
		}

		JCoTable line_item_in = null;
		JCoDestination HFrank = null;
		JCoRepository repository = null;
		JCoFunction jcoFunction = null;

		try {
			HFrank = JCoDestinationManager.getDestination("HFrank");
			HFrank.ping();
			repository = HFrank.getRepository();
			jcoFunction = repository.getFunction(RFCName);

			// System.out.println("repository --" + repository);
		} catch (Exception e) {
			log.error("Exception occured 1:" + e.fillInStackTrace());
		}

		try {
			// Pass the key and value to retrieve data from SAP

			if (SAPColumnHeads.containsKey("IMPORTKEYS")) {
				String importKeys[] = (String[]) SAPColumnHeads.get("IMPORTKEYS");
				ArrayList importValues = (ArrayList) SAPValues.get("IMPORTVALUES");

				// Log.printLog(application, "IMPORTKEYS : ", 2);

				for (int j = 0; j < importValues.size(); j++) {
					String importVals[] = (String[]) importValues.get(j);

					for (int k = 0; k < importKeys.length; k++) {
						// setting import parameter
						jcoFunction.getImportParameterList().setValue(importKeys[k], importVals[k]);
					}
				}
			}

			if (SAPConnectionDetails.containsKey("IMPORTHEADER")) {

				String headName = (String) SAPConnectionDetails.get("IMPORTHEADER");

				if (headName != null && !"".equals(headName.trim())) {

					JCoStructure header_in = jcoFunction.getImportParameterList().getStructure(headName);
					String[] headItem = (String[]) SAPColumnHeads.get("IMPORTHEADITEM");
					ArrayList headItemList = (ArrayList) SAPValues.get("IMPORTHEADITEMLIST");
					String headItemValue[] = (String[]) headItemList.get(0);

					for (int k = 0; k < headItem.length; k++) {
						if (headItemValue[k].equals("-"))
							headItemValue[k] = "";

						header_in.setValue(headItem[k], headItemValue[k]);
					}
				}
			}

			if (SAPConnectionDetails.containsKey("TOTALIMPORTTABLESTOSET")) {
				String totalTables = (String) SAPConnectionDetails.get("TOTALIMPORTTABLESTOSET");
				int noOfTables = Integer.parseInt(totalTables);

				String tableName = "";
				ArrayList lineItemList = new ArrayList();
				String[] lineItem1 = null;

				for (int mm = 1; mm <= noOfTables; mm++) {
					String tableNameNo = "IMPORTTABLENAME" + mm;
					String lineItemNo = "IMPORTLINEITEMLIST" + mm;
					String lineItemColsNo = "IMPORTLINEITEM" + mm;

					tableName = (String) SAPConnectionDetails.get(tableNameNo);
					lineItemList = (ArrayList) SAPValues.get(lineItemNo);
					lineItem1 = (String[]) SAPColumnHeads.get(lineItemColsNo);

					line_item_in = jcoFunction.getTableParameterList().getTable(tableName);

					for (int j = 0; j < lineItemList.size(); j++) {

						line_item_in.appendRow();
						String lineItemValue[] = (String[]) lineItemList.get(j);

						for (int m = 0; m < lineItem1.length; m++) {
							if (lineItemValue[m].equals("-"))
								lineItemValue[m] = "";
							line_item_in.setValue(lineItem1[m], lineItemValue[m]);
						}
					}

				} // end of for
			}
		} catch (Exception e) {
			log.error("exception 2 :" + e.fillInStackTrace());
		}

		try {
			jcoFunction.execute(HFrank);
		} catch (Exception e) {
			log.error("exception 3 :" + e.fillInStackTrace());
		}

		Hashtable SAPDataHash = new Hashtable();

		try {
			// Fetch data from Table returned by SAP
			if (SAPColumnHeads.containsKey("EXPORTKEYS")) {
				String exportKeys[] = (String[]) SAPColumnHeads.get("EXPORTKEYS");

				for (int j = 0; j < exportKeys.length; j++) {
					SAPDataHash.put(exportKeys[j], jcoFunction.getExportParameterList().getString(exportKeys[j]));
				}
			}

			if (SAPConnectionDetails.containsKey("RETURNTABLE")) {
				String returnTable = (String) SAPConnectionDetails.get("RETURNTABLE");

				ArrayList returnData = new ArrayList();

				JCoTable jcoReturn = jcoFunction.getTableParameterList().getTable(returnTable);

				String[] returnKeys = (String[]) SAPColumnHeads.get("RETURNKEYS");

				for (int i = 0; i < jcoReturn.getNumRows(); i++) {
					jcoReturn.setRow(i);
					String[] tmpArr = new String[returnKeys.length];

					for (int m = 0; m < returnKeys.length; m++) {
						tmpArr[m] = jcoReturn.getString(returnKeys[m]);
					}

					returnData.add(tmpArr);
				}

				SAPDataHash.put("RETURNDATA", returnData);

			}

			if (SAPConnectionDetails.containsKey("OUTPUT")) {
				String returnOutput = (String) SAPConnectionDetails.get("OUTPUT");
				String outputData = jcoFunction.getExportParameterList().getString(returnOutput);

				SAPDataHash.put("OUTPUTKEY", outputData);

			}

			if (SAPConnectionDetails.containsKey("RETURNEXPORTTABLE")) {
				String returnTable = (String) SAPConnectionDetails.get("RETURNEXPORTTABLE");

				ArrayList returnData = new ArrayList();

				JCoTable jcoReturn = jcoFunction.getExportParameterList().getTable(returnTable);

				String[] returnKeys = (String[]) SAPColumnHeads.get("RETURNKEYS");

				for (int i = 0; i < jcoReturn.getNumRows(); i++) {
					jcoReturn.setRow(i);
					String[] tmpArr = new String[returnKeys.length];

					for (int m = 0; m < returnKeys.length; m++) {
						tmpArr[m] = jcoReturn.getString(returnKeys[m]);
					}

					returnData.add(tmpArr);
				}
				SAPDataHash.put("RETURNDATA", returnData);
			}

			if (SAPConnectionDetails.containsKey("TOTALTABLESTORETURN")) {
				String totalTables = (String) SAPConnectionDetails.get("TOTALTABLESTORETURN");
				int noOfTables = Integer.parseInt(totalTables);

				log.info("TOTALTABLESTORETURN : " + noOfTables);

				for (int mm = 1; mm <= noOfTables; mm++) {

					String returnTable = (String) SAPConnectionDetails.get("RETURNTABLE" + mm);

					log.info("returnTable : " + mm + "----" + returnTable);

					ArrayList returnData = new ArrayList();

					JCoTable jcoReturn = jcoFunction.getTableParameterList().getTable(returnTable);

					String[] returnKeys = (String[]) SAPColumnHeads.get("RETURNKEYS" + mm);

					log.info("returnKeys : ----" + returnKeys);

					for (int i = 0; i < jcoReturn.getNumRows(); i++) {
						jcoReturn.setRow(i);
						String[] tmpArr = new String[returnKeys.length];

						for (int m = 0; m < returnKeys.length; m++) {
							tmpArr[m] = jcoReturn.getString(returnKeys[m]);

							log.info(SAPDataHash);
						}

						returnData.add(tmpArr);
					}
					SAPDataHash.put("RETURNDATA" + mm, returnData);
				}
			}

			if (SAPConnectionDetails.containsKey("TOTALEXPORTTABLESTORETURN")) {
				String totalTables = (String) SAPConnectionDetails.get("TOTALEXPORTTABLESTORETURN");
				int noOfTables = Integer.parseInt(totalTables);

				for (int mm = 1; mm <= noOfTables; mm++) {

					String returnTable = (String) SAPConnectionDetails.get("RETURNTABLE" + mm);

					ArrayList returnData = new ArrayList();

					JCoTable jcoReturn = jcoFunction.getExportParameterList().getTable(returnTable);

					String[] returnKeys = (String[]) SAPColumnHeads.get("RETURNKEYS" + mm);

					for (int i = 0; i < jcoReturn.getNumRows(); i++) {
						jcoReturn.setRow(i);
						String[] tmpArr = new String[returnKeys.length];

						for (int m = 0; m < returnKeys.length; m++) {
							tmpArr[m] = jcoReturn.getString(returnKeys[m]);

							log.info("tmpArr[m]  : " + m + "----" + tmpArr[m]);
						}

						returnData.add(tmpArr);
					}

					SAPDataHash.put("RETURNDATA" + mm, returnData);
				}
			}
		} catch (Exception e) {
			log.error("Exception 4 :" + e.fillInStackTrace());
		}

		return SAPDataHash;
	}

}