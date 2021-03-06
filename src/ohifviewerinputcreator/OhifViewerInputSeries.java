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

/********************************************************************
* @author Simon J Doran
* Java class: OhifViewerInputSeries.java
* First created on Sep 12, 2017 at 11:10:36 AM
* 
* Component of OhifViewerInput, which is serialised to JSON by
* CreateOhifViewerInputJson.java
* 
*********************************************************************/

package ohifviewerinputcreator;

import java.util.List;

public class OhifViewerInputSeries
{	
	private String  seriesInstanceUid;
	private String  seriesDescription;
	private Integer seriesNumber;
	private List<OhifViewerInputInstance> instances;
	
	
	public String getSeriesInstanceUid()
	{
		return seriesInstanceUid;
	}

	public void setSeriesInstanceUid(String seriesInstanceUid)
	{
		this.seriesInstanceUid = seriesInstanceUid;
	}

	public String getSeriesDescription()
	{
		return seriesDescription;
	}

	public void setSeriesDescription(String seriesDescription)
	{
		this.seriesDescription = seriesDescription;
	}

	public Integer getSeriesNumber()
	{
		return seriesNumber;
	}

	public void setSeriesNumber(Integer seriesNumber)
	{
		this.seriesNumber = seriesNumber;
	}

	public List<OhifViewerInputInstance> getInstances()
	{
		return instances;
	}

	public void setInstances(List<OhifViewerInputInstance> instances)
	{
		this.instances = instances;
	}
	
}
