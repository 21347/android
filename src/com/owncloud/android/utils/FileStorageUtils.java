/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.files.RemoteFile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.Log;


/**
 * Static methods to help in access to local file system.
 * 
 * @author David A. Velasco
 */
public class FileStorageUtils {
    //private static final String LOG_TAG = "FileStorageUtils";
	
  
    /**
     * Function to read the global base path for storing all data, accounts and logs. Only one
     * path for all accounts.
     * Defaults to the first external storage found, which might actually be non-external on modern devices.
     * @param ctxt is the Context to load the preferences from
     * @return absolute path to the local storage 
     */
    public static final String getBasePath(Context ctxt)
    {
        String path = "";
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctxt);
        if (settings.contains("local_base_path"))
        {
            try
            {
                path = settings.getString("local_base_path", "");
            }
            catch (Exception e)
            {
                Log.e("owncloud", "Error loading preference 'local_base_path', will use default of local-storage!");
                path = "";
            }
        }
        
        if (path.equals(""))
        {
            //Load default:
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        
        //No slash at the end...
        if (path.endsWith(File.separator)) path = path.substring(0, path.length()-2);
        
        Log.v("owncloud", "Using base path: " + path);
        
        return path;
    }

    public static final String getSavePath(String accountName, Context ctxt) {
        File sdCard = Environment.getExternalStorageDirectory();
        return sdCard.getAbsolutePath() + "/" + MainApp.getDataFolder() + "/" + Uri.encode(accountName, "@");
        // URL encoding is an 'easy fix' to overcome that NTFS and FAT32 don't allow ":" in file names, that can be in the accountName since 0.1.190B
    }
	
    public static final String getSavePath(String accountName) {
        return getSavePath(accountName, MainApp.getAppContext());
    }
 
    public static final String getDefaultSavePathFor(String accountName, OCFile file, Context ctxt) {
        return getSavePath(accountName, ctxt) + file.getRemotePath();
    }
    public static final String getDefaultSavePathFor(String accountName, OCFile file) {
        return getDefaultSavePathFor(accountName, file, MainApp.getAppContext());
    }


    public static final String getTemporalPath(String accountName, Context ctxt) {
        return getBasePath(ctxt) + File.separator + MainApp.getDataFolder() + File.separator + "tmp" + 
                File.separator + Uri.encode(accountName, "@");
            // URL encoding is an 'easy fix' to overcome that NTFS and FAT32 don't allow ":" in file names, that can be in the accountName since 0.1.190B
    }
    public static final String getTemporalPath(String accountName) {
        return getTemporalPath(accountName, MainApp.getAppContext());
    }

    @SuppressLint("NewApi")
    public static final long getUsableSpace(String accountName, Context ctxt) {
        File savePath = new File(getSavePath(accountName, ctxt));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
            return savePath.getUsableSpace();

        } else {
            StatFs stats = new StatFs(savePath.getAbsolutePath());
            return stats.getAvailableBlocks() * stats.getBlockSize();
        }

    }
    public static final long getUsableSpace(String accountName) {
        return getUsableSpace(accountName, MainApp.getAppContext());
    }
     
    public static final String getLogPath(Context ctxt)  {
        return getBasePath(ctxt) + File.separator + MainApp.getDataFolder() + File.separator + "log";
    }
    public static final String getLogPath() {
        return getLogPath(MainApp.getAppContext());
	}

    public static String getInstantUploadFilePath(Context context, String fileName) {
        String uploadPath = context.getString(R.string.instant_upload_path);
        String value = uploadPath + OCFile.PATH_SEPARATOR +  (fileName == null ? "" : fileName);
        return value;
    }
    
    public static String getParentPath(String remotePath) {
        String parentPath = new File(remotePath).getParent();
        parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath : parentPath + OCFile.PATH_SEPARATOR;
        return parentPath;
    }
    
    /**
     * Creates and populates a new {@link OCFile} object with the data read from the server.
     * 
     * @param remote    remote file read from the server (remote file or folder).
     * @return          New OCFile instance representing the remote resource described by we.
     */
    public static OCFile fillOCFile(RemoteFile remote) {
        OCFile file = new OCFile(remote.getRemotePath());
        file.setCreationTimestamp(remote.getCreationTimestamp());
        file.setFileLength(remote.getLength());
        file.setMimetype(remote.getMimeType());
        file.setModificationTimestamp(remote.getModifiedTimestamp());
        file.setEtag(remote.getEtag());
        
        return file;
    }
    
    /**
     * Creates and populates a new {@link RemoteFile} object with the data read from an {@link OCFile}.
     * 
     * @param oCFile    OCFile 
     * @return          New RemoteFile instance representing the resource described by ocFile.
     */
    public static RemoteFile fillRemoteFile(OCFile ocFile){
        RemoteFile file = new RemoteFile(ocFile.getRemotePath());
        file.setCreationTimestamp(ocFile.getCreationTimestamp());
        file.setLength(ocFile.getFileLength());
        file.setMimeType(ocFile.getMimetype());
        file.setModifiedTimestamp(ocFile.getModificationTimestamp());
        file.setEtag(ocFile.getEtag());
        return file;
    }
  
    public static void copyDirectory(File sourceLocation , File targetLocation)
            throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists() && !targetLocation.mkdirs()) {
                throw new IOException("Cannot create dir " + targetLocation.getAbsolutePath());
            }
    
            String[] children = sourceLocation.list();
            for (int i=0; i<children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {
    
            // make sure the directory we plan to store the recording in exists
            File directory = targetLocation.getParentFile();
            if (directory != null && !directory.exists() && !directory.mkdirs()) {
                throw new IOException("Cannot create dir " + directory.getAbsolutePath());
            }
    
            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);
    
            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }
}