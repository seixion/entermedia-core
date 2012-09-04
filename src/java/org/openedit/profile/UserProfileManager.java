package org.openedit.profile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.ListHitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.Group;
import com.openedit.users.User;

public class UserProfileManager
{
	protected SearcherManager fieldSearcherManager;
	private static final Log log = LogFactory.getLog(UserProfileManager.class);

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public UserProfile loadUserProfile(WebPageRequest inReq, String inCatalogId, String inUserName)
	{
		if( inUserName == null )
		{
			inUserName = "anonymous";
		}
		boolean reload = Boolean.parseBoolean( inReq.getRequestParameter("reloadprofile") );
		UserProfile userprofile = (UserProfile)inReq.getPageValue("userprofile");
		if( !reload && userprofile != null)
		{
			return userprofile;
		}
		String id = inCatalogId + "userprofile" + inUserName;
		if( !reload )
		{
			userprofile = (UserProfile)inReq.getSessionValue(id);
		}
		if( !reload && userprofile != null && inUserName.equals(userprofile.getUserId()) )
		{
			//check searcher cache?
			inReq.putPageValue("userprofile", userprofile); 

			return userprofile;
		}
		if(inCatalogId == null)
		{
			return null;
		}
		Searcher searcher = getSearcherManager().getSearcher(inCatalogId, "userprofile");
		userprofile = (UserProfile)searcher.searchByField("userid", inUserName);
		if( userprofile == null)
		{
			userprofile = (UserProfile)searcher.createNewData();
			userprofile.setProperty("userid", inUserName);
			if( inUserName.equals("admin"))
			{
				userprofile.setProperty("settingsgroup","administrator");
			}
			else
			{
				userprofile.setProperty("settingsgroup","guest");
			}
			userprofile.setSourcePath(inUserName);
			userprofile.setCatalogId(inCatalogId);
			searcher.saveData(userprofile, inReq.getUser());
		}
		userprofile.setSourcePath(inUserName);
		userprofile.setCatalogId(inCatalogId);
		
		
		List ok = new ArrayList();
		List okUpload = new ArrayList();
		
		//check the parent first, then the appid
		String appid = inReq.findValue("parentapplicationid");
		if( appid == null)
		{
			appid = inReq.findValue("applicationid");
		}
		
		Collection catalogs = getSearcherManager().getSearcher(appid, "catalogs").getAllHits();
		
		for (Iterator iterator = catalogs.iterator(); iterator.hasNext();)
		{
			Data cat = (Data) iterator.next();
			//MediaArchive archive = getMediaArchive(cat.getId());
			WebPageRequest catcheck = inReq.getPageStreamer().canDoPermissions("/" + cat.getId());
			Boolean canview = (Boolean)catcheck.getPageValue("canview");
			if( canview != null && canview)
			{
				ok.add(cat);
			}
			Boolean canupload = (Boolean)catcheck.getPageValue("canupload");
			if(canupload != null && canupload)
			{
				okUpload.add(cat);
			}
		}
		userprofile.setCatalogs(new ListHitTracker(ok));
		userprofile.setUploadCatalogs(new ListHitTracker(okUpload));
		loadLibraries(userprofile, inCatalogId );

		if (inReq.getUserName().equals(userprofile.getUserId()))
		{
			inReq.putSessionValue(id, userprofile);
		}
		
		inReq.putPageValue("userprofile", userprofile); 
		return userprofile;
	}

	protected void loadLibraries(UserProfile inUserprofile, String inCatalogId)
	{
		Set<String> all = new HashSet<String>();
		Searcher searcher = getSearcherManager().getSearcher(inCatalogId, "libraryusers");
		Collection found = searcher.fieldSearch("userid", inUserprofile.getUserId());
		
		for (Iterator iterator = found.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			all.add(data.get("libraryid"));
		}

		if( !"anonymous".equals( inUserprofile.getUserId() ) )
		{
			searcher = getSearcherManager().getSearcher(inCatalogId, "librarygroups");
			
			SearchQuery query = searcher.createSearchQuery();
			StringBuffer groups = new StringBuffer();
			
			User user = (User)getSearcherManager().getData("system", "user",inUserprofile.getUserId());
			if( user != null )
			{
				for (Iterator iterator = user.getGroups().iterator(); iterator.hasNext();)
				{
					Group group = (Group) iterator.next();
					groups.append(group.getId());
					if( iterator.hasNext() )
					{
						groups.append(" ");
					}
				}
				query.addOrsGroup("groupid", groups.toString() );
				found = searcher.search(query);
				for (Iterator iterator = found.iterator(); iterator.hasNext();)
				{
					Data data = (Data) iterator.next();
					all.add(data.get("libraryid"));
				}
			}
		}		

		searcher = getSearcherManager().getSearcher(inCatalogId, "libraryroles");
		if( inUserprofile.getSettingsGroup() != null )
		{
			found = searcher.fieldSearch("roleid", inUserprofile.getSettingsGroup().getId());
			
			for (Iterator iterator = found.iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				all.add(data.get("libraryid"));
			}
		}
		inUserprofile.setCombinedLibraries(all);
	}

	public void saveUserProfile(UserProfile inUserProfile)
	{
		if( "anonymous".equals( inUserProfile.getUserId() ) )
		{
			return;
		}
		Searcher searcher = getSearcherManager().getSearcher(inUserProfile.getCatalogId(), "userprofile");
		if( inUserProfile.getSourcePath() == null )
		{
			throw new OpenEditException("user profile source path is null");
		}
		searcher.saveData(inUserProfile, null);
	}
}
