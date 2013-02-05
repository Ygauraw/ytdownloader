package dentex.youtube.downloader;

import group.pals.android.lib.ui.filechooser.FileChooserActivity;
import group.pals.android.lib.ui.filechooser.io.localfile.LocalFile;
import group.pals.android.lib.ui.filechooser.services.IFileProvider;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

public class SettingsActivity extends Activity {
	
	private static final int _ReqChooseFile = 0;
	public static String chooserSummary;
    public static SharedPreferences settings;
	public static final String PREFS_NAME = "dentex.youtube.downloader_preferences";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load default preferences values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        
    	settings = getSharedPreferences(PREFS_NAME, 0);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
    
    public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
    	
    	private static final String DEBUG_TAG = "SettingsActivity";
		private Preference filechooser;
		private Preference quickStart;
		private int icon;

		
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);
            String cf = settings.getString("CHOOSER_FOLDER", "");
            if (cf.isEmpty()) {
            	chooserSummary = getString(R.string.chooser_location_summary_init);
            } else {
            	chooserSummary = settings.getString("CHOOSER_FOLDER", "");
            }
            initSwapPreference();
            initBitratePreference();
            
            for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
                initSummary(getPreferenceScreen().getPreference(i));
            }

            filechooser = (Preference) getPreferenceScreen().findPreference("open_chooser");
            filechooser.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	Intent intent = new Intent(getActivity(),  FileChooserActivity.class);
            		intent.putExtra(FileChooserActivity._Rootpath, (Parcelable) new LocalFile(Environment.getExternalStorageDirectory()));
            		intent.putExtra(FileChooserActivity._FilterMode, IFileProvider.FilterMode.DirectoriesOnly);
            		startActivityForResult(intent, _ReqChooseFile);
                    return true;
                }
            });
            
            quickStart = (Preference) getPreferenceScreen().findPreference("quick_start");
            quickStart.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				public boolean onPreferenceClick(Preference preference) {
					showPopUp(getString(R.string.quick_start_title), getString(R.string.quick_start_text), "info");
					return true;
				}
			});
        }

		private void initSwapPreference() {
			boolean swap = settings.getBoolean("swap_location", false);
			PreferenceScreen p = (PreferenceScreen) findPreference("open_chooser");
            if (swap == true) {
            	p.setEnabled(true);
            } else {
            	p.setEnabled(false);
            }
		}
		
		private void initBitratePreference() {
			String encode = settings.getString("audio_extraction_type", "strip");
			Preference p = (Preference) findPreference("mp3_bitrate");
            if (encode.equals("encode") == true) {
            	p.setEnabled(true);
            } else {
            	p.setEnabled(false);
            }
		}
        
        @Override
        public void onResume(){
        	super.onResume();
        	// Set up a listener whenever a key changes            
        	getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }
       
        @Override
        public void onPause() {
        	super.onPause();
        	// Unregister the listener whenever a key changes            
        	getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);    
        }
        
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        	updatePrefSummary(findPreference(key));
        	initSwapPreference();
        	initBitratePreference();
        }
        	 
        private void initSummary(Preference p){
        	if (p instanceof PreferenceCategory){
        		PreferenceCategory pCat = (PreferenceCategory)p;
        		for(int i=0;i<pCat.getPreferenceCount();i++){
        			initSummary(pCat.getPreference(i));
        	    }
        	}else{
        		updatePrefSummary(p);
        	}
        }
        
        private void updatePrefSummary(Preference p){
        	if (p instanceof ListPreference) {
        		ListPreference listPref = (ListPreference) p;
        	    p.setSummary(listPref.getEntry());
        	}
        	if (p instanceof PreferenceScreen) {
        		p.setSummary(chooserSummary);
        	}
        }

        @Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
            case _ReqChooseFile:
                if (resultCode == RESULT_OK) {
                    @SuppressWarnings("unchecked")
					List<LocalFile> files = (List<LocalFile>) data.getSerializableExtra(FileChooserActivity._Results);
                    for (File f : files) {
                    	if (f.canWrite()) {
                        	Log.d(DEBUG_TAG, "Chosen folder is writable");
                        	Pattern extPattern = Pattern.compile("(extSdCard|sdcard1|emmc)");
                        	Matcher extMatcher = extPattern.matcher(f.toString());
                        	if (extMatcher.find()) {
                        		showPopUp(getString(R.string.attention), getString(R.string.extsdcard_warning), "alert");
                        		Log.d(DEBUG_TAG, "...but it's on removable sdcard");
                        	}
                        } else { 
                    		Log.d(DEBUG_TAG, "Chosen folder is NOT writable");
                    		showPopUp(getString(R.string.attention), getString(R.string.system_warning), "alert");
                        }
                    	
                    	chooserSummary = f.toString();
                    	Log.d(DEBUG_TAG, "file-chooser selection: " + chooserSummary);
                    	for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
                            initSummary(getPreferenceScreen().getPreference(i));
                        }
                    	SharedPreferences.Editor editor = settings.edit();
                    	editor.putString("CHOOSER_FOLDER", chooserSummary);
                    	editor.commit();
                    }
                }
                break;
            }
        }
        
        private void showPopUp(String title, String message, String type) {
            AlertDialog.Builder helpBuilder = new AlertDialog.Builder(getActivity());
            helpBuilder.setTitle(title);
            helpBuilder.setMessage(message);

            if ( type == "alert" ) {
                icon = android.R.drawable.ic_dialog_alert;
            } else if ( type == "info" ) {
                icon = android.R.drawable.ic_dialog_info;
            }

            helpBuilder.setIcon(icon);
            helpBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing but close the dialog
                }
            });

            AlertDialog helpDialog = helpBuilder.create();
            helpDialog.show();
        }

     }
}