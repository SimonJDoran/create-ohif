/********************************************************************
* Copyright (c) 2017, Institute of Cancer Research
* All rights reserved.
* 
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
* 
* (1) Redistributions of source code must retain the above copyright
*     notice, this list of conditions and the following disclaimer.
* 
* (2) Redistributions in binary form must reproduce the above
*     copyright notice, this list of conditions and the following
*     disclaimer in the documentation and/or other materials provided
*     with the distribution.
* 
* (3) Neither the name of the Institute of Cancer Research nor the
*     names of its contributors may be used to endorse or promote
*     products derived from this software without specific prior
*     written permission.
* 
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
* "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
* LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
* FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
* COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
* INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
* SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
* HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
* STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
* OF THE POSSIBILITY OF SUCH DAMAGE.
*********************************************************************/

package ohifviewerinputcreator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import etherj.PathScan;
import etherj.dicom.DicomReceiver;
import etherj.dicom.DicomToolkit;
import etherj.dicom.Patient;
import etherj.dicom.PatientRoot;
import etherj.dicom.Series;
import etherj.dicom.SopInstance;
import etherj.dicom.Study;
import exceptions.XNATException;
import generalUtilities.Vector2D;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import org.dcm4che2.data.DicomObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xnatRestToolkit.XNATRESTToolkit;
import xnatRestToolkit.XNATServerConnection;

/**
 *
 * @author simond
 */
public class CreateOhifViewerInputJson
{
	private static final Logger logger =
		LoggerFactory.getLogger(CreateOhifViewerInputJson.class);

	private final DicomToolkit dcmTk = DicomToolkit.getDefaultToolkit();

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
	{
		CreateOhifViewerInputJson viewer = new CreateOhifViewerInputJson();
		// viewer.run(args[0], args[1], args[2]);
		viewer.run("https://bifrost.icr.ac.uk:8443/XNAT_anonymised", "testuser", "test");
	}

	
	
	private void run(String XnatUrl, String userid, String passwd)
	{
		XNATServerConnection  xnsc;
		// Open a connection to the XNAT server and loop successively over
		// projects, subjects and experiments.
		try
		{
			xnsc = new XNATServerConnection(XnatUrl, userid, passwd);
		}
		catch (MalformedURLException exMU)
		{
			logger.error("Can't open connection to " + XnatUrl + " - malformed URL");
			return;
		}
		
		xnsc.connect();
		XNATRESTToolkit xnrt = new XNATRESTToolkit(xnsc);
		
		Vector2D<String> resultProj;
		try
      {
         String restCommand = "/data/archive/projects?format=xml";
         resultProj         = xnrt.RESTGetResultSet(restCommand);
      }
      catch (XNATException exXNAT)
      {
         logger.error("Problem retrieving list of projects: " + exXNAT.getMessage());
			return;
      }
		

      for (int i=0; i<resultProj.size(); i++)
      {
         String proj = resultProj.atom(0, i);
			System.out.println(proj);
			Vector2D<String> resultSubj;
			try
			{
				String restCommand = "/data/archive/projects/" + proj
											  + "/subjects?format=xml";
				resultSubj         = xnrt.RESTGetResultSet(restCommand);
			}
			catch (XNATException exXNAT)
			{
				logger.error("Problem retrieving list of subjects for project "
					+ proj + " : " + exXNAT.getMessage());
				return;
			}
			
			for (int j=0; j<resultSubj.size(); j++)
			{
				String subj = resultSubj.atom(2, j);
				System.out.println("> " + (subj));
				Vector2D<String> resultExp;
				try
				{
					String restCommand = "/data/archive/projects/" + proj
												  + "/subjects/" + subj
							                 + "/experiments?format=xml";
					resultExp          = xnrt.RESTGetResultSet(restCommand);
				}
				catch (XNATException exXNAT)
				{
					logger.error("Problem retrieving list of experiments for project "
						+ proj +  "and subject " + subj + ": " + exXNAT.getMessage());
					return;
				}

				for (int k=0; k<resultExp.size(); k++)
				{
					String exp = resultExp.atom(5, k);
					System.out.println(">> " + exp);
					
					// Use Etherj to scan the input directory for DICOM files and collate
					// all the required metadata.
					String      basePath = "/data/xnatsimond/" + proj + "/arc001/"
							                  + exp + "/SCANS";
					System.out.println(basePath);
					//PatientRoot root     = scanPath(basePath);
				}
				
			}
			
      }

/*		

		
		// Transform the Etherj output into the structure needed by the
		// OHIF viewer.
		OhifViewerInput ovi = createOhifViewerInput(basePath, transactionId, XnatUrl, root);
		
		// Serialise the viewer input to JSON.
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String serialisedOvi = gson.toJson(ovi);
		
		System.out.println(serialisedOvi);
		
		System.out.println("Here");
		
*/	
		
	}
	
	private PatientRoot scanPath(String path)
	{
		logger.info("DICOM search: "+path);
		DicomReceiver dcmRx = new DicomReceiver();
		PathScan<DicomObject> pathScan = dcmTk.createPathScan();
		pathScan.addContext(dcmRx);
		PatientRoot root = null;
		try
		{
			pathScan.scan(path, true);
			root = dcmRx.getPatientRoot();
		}
		catch (IOException ex)
		{
			logger.warn(ex.getMessage(), ex);
		}
		return root;
	}
	
	
	private OhifViewerInput createOhifViewerInput(String basePath, String transactionId, String XnatUrl, PatientRoot root)
	{
		OhifViewerInput ovi     = new OhifViewerInput();
		List<OhifViewerInputStudy> oviStudyList = new ArrayList<>();
		
		ovi.setTransactionId(transactionId);
		ovi.setStudies(oviStudyList);
		
		List<Patient> patList = root.getPatientList();
		for (Patient pat : patList)
		{
			List<Study> studyList = pat.getStudyList();			
			for (Study std : studyList)
			{
				OhifViewerInputStudy oviStd = new OhifViewerInputStudy();
				oviStudyList.add(oviStd);
				
				oviStd.setStudyInstanceUid(std.getUid());
				oviStd.setPatientName(pat.getName());
				List<OhifViewerInputSeries> oviSeriesList = new ArrayList<>();
				oviStd.setSeriesList(oviSeriesList);
						
				List<Series> seriesList = std.getSeriesList();
				for (Series ser : seriesList)
				{
					OhifViewerInputSeries oviSer = new OhifViewerInputSeries();
					oviSeriesList.add(oviSer);
					
					oviSer.setSeriesInstanceUid(ser.getUid());
					oviSer.setSeriesDescription(ser.getDescription());
					oviSer.setSeriesNumber(ser.getNumber());
					List<OhifViewerInputInstance> oviInstanceList = new ArrayList<>();
					oviSer.setInstances(oviInstanceList);
					
					List<SopInstance> sopList = ser.getSopInstanceList();
					for (SopInstance sop : sopList)
					{
						OhifViewerInputInstance oviInst = new OhifViewerInputInstance();
						oviInstanceList.add(oviInst);
						
						oviInst.setSopInstanceUid(sop.getUid());
						oviInst.setInstanceNumber(sop.getInstanceNumber());
						oviInst.setColumns(sop.getColumnCount());
						oviInst.setRows(sop.getRowCount());
						oviInst.setFrameOfReferenceUID(sop.getFrameOfReferenceUid());
						oviInst.setImagePositionPatient(dbl2DcmString(sop.getImagePositionPatient()));
						oviInst.setImageOrientationPatient(dbl2DcmString(sop.getImageOrientationPatient()));
						oviInst.setUrl("dicomweb://" );
						oviInst.setPixelSpacing(dbl2DcmString(sop.getPixelSpacing()));
					}
				}
			}
		}
		
		return ovi;
	}
	
	
	private String dbl2DcmString(double[] d)
	{
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<d.length; i++)
		{
			sb.append(d[i]);
			if (i != (d.length-1)) sb.append("\\");
		}
		return sb.toString();
	}
}
