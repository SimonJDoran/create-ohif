/********************************************************************
* Copyright (c) 2018, Institute of Cancer Research
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
import java.io.File;
import java.util.List;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
   
   private static final String SEP = File.separator; 

	private final DicomToolkit dcmTk = DicomToolkit.getDefaultToolkit();

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
	{
		CreateOhifViewerInputJson viewer = new CreateOhifViewerInputJson();
		viewer.run(args[0], args[1], args[2], args[3]);
	}

	
	
	private void run(String xnatUrl, String xnatArchivePath, String userid,
                    String passwd)
	{
		XNATServerConnection  xnsc;
      String protocol;
		// Open a connection to the XNAT server and loop successively over
		// projects, subjects and experiments.
		try
		{
			xnsc = new XNATServerConnection(xnatUrl, userid, passwd);
         protocol = (new URL(xnatUrl)).getProtocol();
		}
		catch (MalformedURLException exMU)
		{
			logger.error("Can't open connection to " + xnatUrl + " - malformed URL");
			return;
		}
		
		xnsc.connect();
		XNATRESTToolkit xnrt = new XNATRESTToolkit(xnsc);
		
		// Note that Vector2D is a very "un-Javalike" class that I wrote when
		// I had only a few week's programming experience of Java. It's now
		// deprecated(!) but was the basis of the very useful XNATRESTToolkit
		// package. All due for refactoring when I get time.
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
		

      // Initial demonstrator creates new JSONs for everything in the XNAT database!
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
					String expId    = resultExp.atom(0, k);
               String expLabel = resultExp.atom(5, k);
					System.out.println(">> " + expLabel);
					
					// Use Etherj to scan the input directory for DICOM files and collate
					// all the required metadata.
					// xnatScanPath gives the actual base path in the filesystem under
               // which the scan data are stored, whereas xnatScanUrl gives the
               // route to get at the data via dicomweb/REST.
					String      xnatScanPath = xnatArchivePath + SEP + proj + SEP + "arc001"
							                     + SEP + expLabel + SEP + "SCANS";
               
               String      xnatScanUrl  = xnatUrl.replace(protocol, "dicomweb")
                                          + "/data/archive/projects" + proj
                                          + "/subject/" + subj
                                          + "/experiments/" + expId
                                          + "/scans/";
					
					String      jsonData     = jsonify(xnatScanPath, xnatScanUrl, expId);
				
					// Now go and do something with it!
					
				}	
			}		
      }
	}
	
	
	private PatientRoot scanPath(String path)
	{
		logger.info("DICOM search: " + path);
		
		DicomReceiver         dcmRec   = new DicomReceiver();
		PathScan<DicomObject> pathScan = dcmTk.createPathScan();
		
		pathScan.addContext(dcmRec);
		PatientRoot root = null;
		try
		{
			pathScan.scan(path, true);
			root = dcmRec.getPatientRoot();
		}
		catch (IOException ex)
		{
			logger.warn(ex.getMessage(), ex);
		}
		return root;
	}
	
	
	

	private String jsonify(String xnatScanPath, String xnatScanUrl, String transactionId)
	{
		// Use Etherj to do the heavy lifting of sifting through all the scan data.
		PatientRoot root = scanPath(xnatScanPath);
					
		// Transform the Etherj output into the structure needed by the
		// OHIF viewer.
		OhifViewerInput ovi = createOhifViewerInput(transactionId, xnatScanUrl, root);
		
		// Serialise the viewer input to JSON.
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String serialisedOvi = gson.toJson(ovi);
		
		return serialisedOvi;
	}
	
	
	
	
	private OhifViewerInput createOhifViewerInput(String transactionId, String xnatScanUrl, PatientRoot root)
	{
		OhifViewerInput ovi = new OhifViewerInput();
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
						oviInst.setPixelSpacing(dbl2DcmString(sop.getPixelSpacing()));
						
						// Here's the bit that needs changing when we decide exactly how we want to store the files.
						String file = new File(sop.getPath()).getName();
						oviInst.setUrl(xnatScanUrl + ser.getNumber() + "/resources/DICOM/files/" + file);						
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

