package org.witness.ssc.video;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

public class RegionTrail {

	
	private HashMap<Integer,ObscureRegion> regionMap = new HashMap<Integer,ObscureRegion>();
	
	private int startTime = 0;
	private int endTime = 0;
	

	public static final String OBSCURE_MODE_REDACT = "black";
	public static final String OBSCURE_MODE_PIXELATE = "pixel";
	
	private String obscureMode = OBSCURE_MODE_REDACT;
	
	public String getObscureMode() {
		return obscureMode;
	}

	public void setObscureMode(String obscureMode) {
		this.obscureMode = obscureMode;
	}

	public RegionTrail (int startTime, int endTime)
	{
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public int getStartTime() {
		return startTime;
	}

	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}

	public int getEndTime() {
		return endTime;
	}

	public void setEndTime(int endTime) {
		this.endTime = endTime;
	}

	public void addRegion (ObscureRegion or)
	{
		regionMap.put(or.timeStamp,or);
		or.setRegionTrail(this);
	}
	
	public void removeRegion (ObscureRegion or)
	{
		regionMap.remove(or.timeStamp);
	}
	
	public Iterator<ObscureRegion> getRegionsIterator ()
	{
		return regionMap.values().iterator();
	}
	
	public ObscureRegion getRegion (Integer key)
	{
		return regionMap.get(key);
	}
	
	public TreeSet<Integer> getRegionKeys ()
	{
		TreeSet<Integer> regionKeys = new TreeSet<Integer>(regionMap.keySet());

		return regionKeys;
	}
	
	public ObscureRegion getCurrentRegion (int time)
	{
		
		if (time < startTime || time > endTime)
			return null;
		else if (regionMap.size() > 0)
		{
		
			
			TreeSet<Integer> regionKeys = new TreeSet<Integer>(regionMap.keySet());
			
			Integer lastRegionKey = -1;
			
			for (Integer regionKey:regionKeys)
			{
				int comp = regionKey.compareTo(time);
				lastRegionKey = regionKey;
				
				if (comp == 0 || comp == 1)
					break; //it is a match!
				
			}
			
			return regionMap.get(lastRegionKey);
		}
		else
			return null;
	}
}
