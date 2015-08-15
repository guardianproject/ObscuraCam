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
	
	private String obscureMode = OBSCURE_MODE_PIXELATE;
	
	private boolean doTweening = true;
	
	public boolean isDoTweening() {
		return doTweening;
	}

	public void setDoTweening(boolean doTweening) {
		this.doTweening = doTweening;
	}

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
	
	public boolean isWithinTime (int time)
	{

		if (time < startTime || time > endTime)
			return false;
		else
			return true;
	}
	
	public ObscureRegion getCurrentRegion (int time, boolean doTween)
	{

		ObscureRegion regionResult = null;
		
		if (time < startTime || time > endTime)
			return null;
		else if (regionMap.size() > 0)
		{
		
			
			TreeSet<Integer> regionKeys = new TreeSet<Integer>(regionMap.keySet());
			
			Integer lastRegionKey = -1, regionKey = -1;
			
			Iterator<Integer> itKeys = regionKeys.iterator();
			
			while (itKeys.hasNext())
			{
				regionKey = itKeys.next();
				int comp = regionKey.compareTo(time);
				
				if (comp == 0 || comp == 1)
				{
					ObscureRegion regionThis = regionMap.get(regionKey);
					
					if (lastRegionKey != -1 && doTween)
					{
						ObscureRegion regionLast = regionMap.get(lastRegionKey);
					
						float sx, sy, ex, ey;
						
						int timeDiff = regionThis.timeStamp - regionLast.timeStamp;
						int timePassed = time - regionLast.timeStamp;
						
						float d = ((float)timePassed) / ((float)timeDiff);
						
						sx = regionLast.sx + ((regionThis.sx-regionLast.sx)*d);
						sy = regionLast.sy + ((regionThis.sy-regionLast.sy)*d);
						
						ex = regionLast.ex + ((regionThis.ex-regionLast.ex)*d);
						ey = regionLast.ey + ((regionThis.ey-regionLast.ey)*d);
						
						regionResult = new ObscureRegion(time, sx, sy, ex, ey);
						
					}
					else
						regionResult = regionThis;
					
					
					break; //it is a match!
				}
			

				lastRegionKey = regionKey;
				
			}
			
			if (regionResult == null)
				regionResult = regionMap.get(lastRegionKey);
			
			
		}
		
		return regionResult;
	}
}
