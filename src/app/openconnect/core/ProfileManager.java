package app.openconnect.core;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import app.openconnect.VpnProfile;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class ProfileManager {
	public static final String TAG = "OpenConnect";

	private static final String PROFILE_PFX = "profile-";
	private static HashMap<String,VpnProfile> mProfiles;

	private static Context mContext;
	private static SharedPreferences mAppPrefs;

	private static final String ON_BOOT_PROFILE = "onBootProfile";
	private static final String RESTART_ON_BOOT = "restartvpnonboot";

	private static VpnProfile mLastConnectedVpn=null;

	public static void init(Context context) {
		mContext = context;
		mAppPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mProfiles = new HashMap<String, VpnProfile>();

		File prefsdir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
	    if (prefsdir.exists() && prefsdir.isDirectory()) {
	    	for (String s : prefsdir.list()) {
	    		if (s.startsWith(PROFILE_PFX)) {
	    			SharedPreferences p = context.getSharedPreferences(s.replaceFirst(".xml", ""),
	    					Activity.MODE_PRIVATE);
	    			VpnProfile entry = new VpnProfile(p);
	    			if (!entry.isValid()) {
	    				Log.w(TAG, "removing bogus profile '" + s + "'");
	    				File f = new File(s);
	    				f.delete();
	    			} else {
	    				mProfiles.put(entry.getUUIDString(), entry);
	    			}
	    		}
	    	}
	    }
	}

	public synchronized static Collection<VpnProfile> getProfiles() {
		init(mContext);
		return mProfiles.values();
	}

	public synchronized static VpnProfile get(String key) {
		return key == null ? null : mProfiles.get(key);
	}

	public static String getPrefsName(String uuid) {
		return PROFILE_PFX + uuid;
	}

	public synchronized static VpnProfile create(String name) {
		if (getProfileByName(name) != null) {
			return null;
		}
		String uuid = UUID.randomUUID().toString();
		SharedPreferences p = mContext.getSharedPreferences(getPrefsName(uuid), Activity.MODE_PRIVATE);
		VpnProfile profile = new VpnProfile(p, uuid, name);
		mProfiles.put(uuid, profile);
		return profile;
	}

	public synchronized static VpnProfile getProfileByName(String name) {
		for (VpnProfile vpnp : mProfiles.values()) {
			if(vpnp.getName().equals(name)) {
				return vpnp;
			}
		}
		return null;
	}

	public synchronized static boolean delete(String uuid) {
		VpnProfile profile = get(uuid);
		if (profile == null) {
			Log.w(TAG, "error looking up profile " + uuid);
			return false;
		}
		mProfiles.remove(uuid);

		File f = new File(mContext.getApplicationInfo().dataDir + File.separator +
				"shared_prefs" + File.separator + PROFILE_PFX + uuid + ".xml");

		if (f.delete()) {
			Log.i(TAG, "deleted profile " + uuid);
			return true;
		} else {
			Log.w(TAG, "error deleting profile " + uuid);
			return false;
		}
	}

	public synchronized static void setConnectedVpnProfileDisconnected() {
		mLastConnectedVpn = null;
		mAppPrefs.edit()
			.remove(ON_BOOT_PROFILE)
			.commit();
	}

	public synchronized static void setConnectedVpnProfile(VpnProfile connectedProfile) {
		mLastConnectedVpn = connectedProfile;
		mAppPrefs.edit()
			.putString(ON_BOOT_PROFILE, connectedProfile.getUUIDString())
			.commit();
	}

	public synchronized static VpnProfile getOnBootProfile() {
		if (!mAppPrefs.getBoolean(RESTART_ON_BOOT, false)) {
			return null;
		}

		String uuid = mAppPrefs.getString(ON_BOOT_PROFILE, null);
		VpnProfile profile = get(uuid);
		return profile;
	}

	public static VpnProfile getLastConnectedVpn() {
		return mLastConnectedVpn;
	}

}
