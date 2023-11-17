package org.leafylanka.pinpotha.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.github.sundeepk.compactcalendarview.domain.Event;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.leafylanka.pinpotha.R;
import org.leafylanka.pinpotha.models.Post;
import org.leafylanka.pinpotha.models.Utils;

public class AddActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,GoogleApiClient.OnConnectionFailedListener {

    private CompactCalendarView calendarView;
    private TextView txtMonth;
    private EditText edtNote;
    private ProgressDialog mProgressDialog;
    private static final int CAMERA_REQUEST = 1;
    private static final int PICK_FROM_GALLERY = 2;
    private static final int MY_PERMISSIONS_REQUEST_READ_CAMERA = 3;
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTENT = 4 ;
    boolean isCameraOption=true;
    protected GoogleApiClient mGoogleApiClient;
    private Utils mUtils;
    private FloatingActionButton floatingActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        Toolbar toolbar =findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mUtils=new Utils(this,null);
        edtNote=findViewById(R.id.edt_note);
        Button btnAdd=findViewById(R.id.btn_add);
        Button btnSearch=findViewById(R.id.btn_search);

        mProgressDialog =new ProgressDialog(this);
        mProgressDialog.setTitle("Loading...");
        mProgressDialog.setMessage("Posting.....");
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPostsDialog();
            }
        });
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startActivity(new Intent(getApplicationContext(),DashboardActivity.class));

            }
        });

        setTitle(getString(R.string.app_name));

        DrawerLayout drawer =findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView =findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        setUpCalendar();
        getAllPostsTimes();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.not_posted))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    public void setUpCalendar() {
        calendarView=findViewById(R.id.compactcalendar_view);
        txtMonth=findViewById(R.id.txt_month);
        Date currentDate=new Date(Calendar.getInstance().getTimeInMillis());
        txtMonth.setText(mUtils.formatDate(currentDate,"MMM-yyyy"));
        calendarView.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            @Override
            public void onDayClick(Date dateClicked) {
                Intent intent=new Intent(AddActivity.this,PostListActivity.class);
                intent.putExtra("MILLIS",""+dateClicked.getTime());
                startActivity(intent);
            }

            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {
                txtMonth.setText(mUtils.formatDate(firstDayOfNewMonth,"MMM-yyyy"));
            }
        });
    }

    public void getAllPostsTimes() {
        final DatabaseReference allPostsRef= FirebaseDatabase.getInstance().getReference().child("posts")
                .child(FirebaseAuth.getInstance().getCurrentUser().getEmail().replace(".",","));
        allPostsRef.keepSynced(true);
        ValueEventListener listener=new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue()!=null) {
                    for (DataSnapshot snapshot:dataSnapshot.getChildren()) {
                        for (DataSnapshot snapshot1:snapshot.getChildren()) {
                            if (snapshot1.getValue()!=null) {
                                Post post=snapshot1.getValue(Post.class);
                                HashMap<String,Object> timeStamp=post.getTimeStamp();
                                Log.e("tag",timeStamp.get("server_time").toString());
                                Event event = new Event(getResources().getColor(R.color.calendarSelectedDay), Long.parseLong(timeStamp.get("server_time").toString()));
                                calendarView.addEvent(event);
                            }
                        }
                    }
                }
                else {
                    mUtils.showToastMessage(getString(R.string.not_posted));
                }
                allPostsRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mUtils.showToastMessage(getString(R.string.something_went_wrong));
            }
        };
        allPostsRef.addValueEventListener(listener);
    }

    public void showPostsDialog() {
        final String[] option = new String[]{"Take a Note", "Get From Camera",
                "Get From the Phone"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.select_dialog_item, option);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Select Option");
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {


                    if (edtNote.getText().toString().trim().length() > 0) {
                        mUtils.addNote(mProgressDialog,edtNote,true);
                    } else {
                        /*AlertDialog alert = new AlertDialog.Builder(DashboardActivity.this).create();
                        alert.setMessage("Please Type Some good work !!!");
                        alert.show();*/
                        edtNote.setError(getString(R.string.please_type_good_work));
                    }
                }

                if (which == 1) {
                    isCameraOption=true;
                    cameraPermissionCheck();
                }
                if (which == 2) {
                    isCameraOption=false;
                    cameraPermissionCheck();
                }

            }

        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;

        switch (requestCode) {
            case CAMERA_REQUEST:
                mUtils.postWithPhoto(null, edtNote, mProgressDialog, data, true);
                break;

            case PICK_FROM_GALLERY:
                Uri extras2 = data.getData();
                if (extras2 != null) {
                    final Bitmap selectedImage;
                    try {
                        selectedImage = mUtils.decodeUri(this, extras2, 100);
                        mUtils.postWithPhoto(selectedImage, edtNote, mProgressDialog, null, true);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    public void cameraPermissionCheck() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_READ_CAMERA);
        } else {
            storagePermissionCheck();
        }
    }

    public void storagePermissionCheck() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_CONTENT);

        } else {
            if (isCameraOption)mUtils.callCamera();
            else mUtils.callGallery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    storagePermissionCheck();

                }
                return;

            }
            case MY_PERMISSIONS_REQUEST_READ_CONTENT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (isCameraOption) mUtils.callCamera();
                    else mUtils.callGallery();
                }
                break;
            }
        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            isCameraOption=true;
            cameraPermissionCheck();
        } else if (id == R.id.nav_gallery) {
            isCameraOption=false;
            cameraPermissionCheck();
        } else if (id == R.id.nav_sign_out) {
            FirebaseAuth.getInstance().signOut();
            if (mGoogleApiClient!=null) Auth.GoogleSignInApi.signOut(mGoogleApiClient);
            Intent i = new Intent(this,SignInActivity.class);
            startActivity(i);
            finish();
        }

        DrawerLayout drawer =findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
