package aherbener2.mydemos.project;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);
    }
    private boolean isFileManagerInitialized;
    private boolean[] selection;
    private File[] files;
    private List<String> filesList;
    private int filesFoundCount;
    private Button refreshButton;
    private File dir;
    private String currentPath;
    private boolean isLongClick;
    private int selectedItemIndex;
    private String copyPath;
    private int getLength() {
        if(files==null) {
            return 0;
        }
        else {
            return files.length;
        }
    }
    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M&&arePermissionsDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }
        if(!isFileManagerInitialized) {
            currentPath=String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS));
            final String rootPath=currentPath.substring(0,currentPath.lastIndexOf('/'));
            final TextView pathOutput=findViewById(R.id.pathOutput);
            final ListView listView = findViewById(R.id.listView);
            final TextAdapter textAdapter1 = new TextAdapter();
            listView.setAdapter(textAdapter1);
            filesList = new ArrayList<>();
            refreshButton=findViewById(R.id.refresh);
            refreshButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pathOutput.setText(currentPath.substring(currentPath.lastIndexOf('/')+1));
                    dir=new File(currentPath);
                    files=dir.listFiles();
                    filesFoundCount=getLength();
                    selection=new boolean[filesFoundCount];
                    textAdapter1.setSelection(selection);
                    filesList.clear();
                    for(int i=0;i<filesFoundCount;i++) {
                        filesList.add(String.valueOf(files[i].getAbsolutePath()));
                    }
                    textAdapter1.setData(filesList);
                }
            });
            refreshButton.callOnClick();
            final Button goBackButton=findViewById(R.id.goBack);
            goBackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(currentPath.equals(rootPath)) {
                        return;
                    }
                    currentPath=currentPath.substring(0,currentPath.lastIndexOf('/'));
                    refreshButton.callOnClick();
                }
            });
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                    new Handler().postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if(!isLongClick) {
                                if(files[i].isDirectory())
                                {
                                    currentPath = files[i].getAbsolutePath();
                                    refreshButton.callOnClick();
                                }
                            }
                        }
                    },50);
                }
            });
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                    isLongClick=true;
                    selection[i]=!selection[i];
                    textAdapter1.setSelection(selection);
                    int counter=0;
                    for (boolean b : selection) {
                        if (b) {
                            counter++;
                        }
                    }
                    if(counter>0) {
                        if(counter==1) {
                            selectedItemIndex=i;
                            findViewById(R.id.rename).setVisibility(View.VISIBLE);
                            if(!files[selectedItemIndex].isDirectory()) {
                                findViewById(R.id.copy).setVisibility(View.VISIBLE);
                            }
                        } else {
                            findViewById(R.id.copy).setVisibility(View.GONE);
                            findViewById(R.id.rename).setVisibility(View.GONE);
                        }
                        findViewById(R.id.bottomBar).setVisibility(View.VISIBLE);
                    } else {
                        findViewById(R.id.bottomBar).setVisibility(View.GONE);
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isLongClick=false;
                        }
                    },1000);
                    return false;
                }
            });
            final Button deleteButton = findViewById(R.id.delete);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder deleteDialog=new AlertDialog.Builder(MainActivity.this);
                    deleteDialog.setTitle("Delete");
                    deleteDialog.setMessage("Are you sure you want to delete this?");
                    deleteDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            for(int j=0;j<getLength();j++) {
                                if(selection[j]) {
                                    deleteFileOrFolder(files[j]);
                                    selection[j]=false;
                                }
                            }
                            refreshButton.callOnClick();
                        }
                    });
                    deleteDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            refreshButton.callOnClick();
                        }
                    });
                    deleteDialog.show();
                }
            });
            final Button createNewFolder=findViewById(R.id.newFolder);
            createNewFolder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final AlertDialog.Builder newFolderDialog=new AlertDialog.Builder(MainActivity.this);
                    newFolderDialog.setTitle("New Folder");
                    final EditText input=new EditText(MainActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    newFolderDialog.setView(input);
                    newFolderDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            final File newFolder=new File(currentPath+"/"+input.getText());
                            if(!newFolder.exists()) {
                                newFolder.mkdir();
                                refreshButton.callOnClick();
                            }
                        }
                    });
                    newFolderDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            dialogInterface.cancel();
                        }
                    });
                    newFolderDialog.show();
                }
            });
            final Button renameButton=findViewById(R.id.rename);
            renameButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final AlertDialog.Builder renameDialog=new AlertDialog.Builder(MainActivity.this);
                    renameDialog.setTitle("Rename to:");
                    final EditText input=new EditText(MainActivity.this);
                    final String renamePath=files[selectedItemIndex].getAbsolutePath();
                    input.setText(renamePath.substring(renamePath.lastIndexOf('/')));
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    renameDialog.setView(input);
                    renameDialog.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String s=new File(renamePath).getParent()+"/"+input.getText();
                            File newFile=new File(s);
                            new File(renamePath).renameTo(newFile);
                            refreshButton.callOnClick();
                        }
                    });
                    renameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            refreshButton.callOnClick();
                        }
                    });
                    renameDialog.show();
                }
            });
            final Button copyButton=findViewById(R.id.copy);
            copyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    copyPath=files[selectedItemIndex].getAbsolutePath();
                    selection=new boolean[getLength()];
                    textAdapter1.setSelection(selection);
                    findViewById(R.id.paste).setVisibility(View.VISIBLE);
                }
            });
            final Button pasteButton=findViewById(R.id.paste);
            pasteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pasteButton.setVisibility(View.GONE);
                    String dstPath=currentPath+copyPath.substring(0,copyPath.lastIndexOf('/'));
                    copy(new File(copyPath), new File(dstPath));
                    refreshButton.callOnClick();
                }
            });
            isFileManagerInitialized=true;
        } else {
            refreshButton.callOnClick();
        }
    }
    private void copy(File src, File dst) {
        try {
            InputStream in=new FileInputStream(src);
            OutputStream out=new FileOutputStream(dst);
            byte[] buf=new byte[1024];
            int len;
            while((len=in.read(buf))>0)
            {
                out.write(buf,0,len);
            }
            out.close();
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    class TextAdapter extends BaseAdapter {
        private List<String> data = new ArrayList<>();
        private boolean[] selection;
        public void setData(List<String> data) {
            if(data!=null) {
                this.data.clear();
                if(data.size()>0) {
                    this.data.addAll(data);
                }
                notifyDataSetChanged();
            }
        }
        void setSelection(boolean[] selection) {
            if(selection!=null) {
                this.selection=new boolean[selection.length];
                for(int i=0;i<selection.length;i++)
                {
                    this.selection[i]=selection[i];
                }
                notifyDataSetChanged();
            }
        }
        @Override
        public int getCount()
        {
            return data.size();
        }

        @Override
        public String getItem(int i)
        {
            return data.get(i);
        }

        @Override
        public long getItemId(int i)
        {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if(view==null) {
                view= LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item, viewGroup, false);
                view.setTag(new ViewHolder((TextView) view.findViewById(R.id.textItem)));
            }
            ViewHolder holder = (ViewHolder) view.getTag();
            final String item = getItem(i);
            holder.info.setText(item.substring(item.lastIndexOf('/')+1));
            if(selection!=null) {
                if(selection[i]) {
                    holder.info.setBackgroundColor((Color.argb(100,9,9,9)));
                }
                else {
                    holder.info.setBackgroundColor(Color.WHITE);
                }
            }
            return view;
        }
        class ViewHolder {
            TextView info;
            ViewHolder(TextView info)
            {
                this.info=info;
            }
        }
    }
    private void deleteFileOrFolder(File toDelete) {
        if(toDelete.isDirectory()) {
            if(toDelete.list().length==0) {
                toDelete.delete();
            } else {
                String files[]=toDelete.list();
                for(String temp:files) {
                    File fileToDelete=new File(toDelete,temp);
                    deleteFileOrFolder(fileToDelete);
                }
                if(toDelete.list().length==0) {
                    toDelete.delete();
                }
            }
        } else {
            toDelete.delete();
        }
    }
    private static final int REQUEST_PERMISSIONS=1234;

    private static final String[] PERMISSIONS= {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int PERMISSIONS_COUNT=2;
    @SuppressLint("NewApi")
    private boolean arePermissionsDenied() {
        int p=0;
        while(p<PERMISSIONS_COUNT) {
            if(checkSelfPermission(PERMISSIONS[p])!= PackageManager.PERMISSION_GRANTED) {
                return true;
            }
            p++;
        }
        return false;
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQUEST_PERMISSIONS&&grantResults.length>0) {
            if(arePermissionsDenied()) {
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            } else {
                onResume();
            }
        }
    }
}