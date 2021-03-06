package com.subhajitdas.c.editProfile;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.subhajitdas.c.Constants;
import com.subhajitdas.c.R;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;

import id.zelory.compressor.Compressor;

public class ProfileEdit extends AppCompatActivity {

    private ImageView mDp, mCoverImage;
    private EditText mName, mBio, mLocation, mWork, mEducation, mSkills;
    private SharedPreferences mIntentData;
    private ProgressDialog mProgress;
    private FloatingActionButton mDoneFab;

    private StorageReference mProfileImagefRef, mProfileThumRef, mCoverImageRef, mCoverThumRef;
    private DatabaseReference mUserDataRef;

    private String mLastActivity, mProfileId;
    private boolean mDPPressed = false, mCoverPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);
        //Getting data from the caller intent.
        Intent gotIntent = getIntent();
        mLastActivity = gotIntent.getStringExtra(Constants.ACTIVITY);
        mProfileId = gotIntent.getStringExtra(Constants.USERID);
        //Toolbar work.
        Toolbar toolbar = (Toolbar) findViewById(R.id.edit_profile_toolbar);
        toolbar.setTitle("Edit Your Profile");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        //Storing the intent data .
        mIntentData = getSharedPreferences(Constants.INTENT_DATA, MODE_PRIVATE);
        SharedPreferences.Editor editor = mIntentData.edit();
        editor.putString(Constants.ACTIVITY, mLastActivity);
        editor.putString(Constants.USERID, mProfileId);
        editor.apply();
        //UI elements.
        mDp = (ImageView) findViewById(R.id.edit_profile_dp);
        mCoverImage = (ImageView) findViewById(R.id.edit_profile_cover);
        mProgress = new ProgressDialog(this);
        mName = (EditText) findViewById(R.id.edit_profile_name);
        mBio = (EditText) findViewById(R.id.edit_profile_bio);
        mLocation = (EditText) findViewById(R.id.edit_profile_location);
        mWork = (EditText) findViewById(R.id.edit_profile_work);
        mEducation = (EditText) findViewById(R.id.edit_profile_education);
        mSkills = (EditText) findViewById(R.id.edit_profile_skills);
        mDoneFab = (FloatingActionButton) findViewById(R.id.done_fab);
        SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.edit_profile_swiperefresh);

        mSwipeRefreshLayout.setEnabled(false);
        //DB links.
        mProfileImagefRef = FirebaseStorage.getInstance().getReference().child(Constants.PROFILE_IMAGES);     //TODO Profile pic upload destination.
        mProfileThumRef = FirebaseStorage.getInstance().getReference().child(Constants.PROFILE_THUMB);          // TODO profile pic thumbnail.
        mCoverImageRef = FirebaseStorage.getInstance().getReference().child(Constants.COVER_IMAGES);          //TODO Cover pic upload destination.
        mCoverThumRef = FirebaseStorage.getInstance().getReference().child(Constants.COVER_THUMB);              // TODO cover pic thumbnail.
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mUserDataRef = FirebaseDatabase.getInstance().getReference().child(Constants.USER).child(userId);
        mUserDataRef.keepSynced(true);
        //Displaying msg on loading.
        mProgress.setTitle("Please wait");
        mProgress.setMessage("Loading your details.");
        mProgress.setCancelable(false);
        mProgress.show();
        mUserDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(Constants.DP_URL)) {
                    String dpUrl = dataSnapshot.child(Constants.DP_URL).getValue().toString();
                    RequestOptions profileOptions = new RequestOptions();
                    profileOptions.circleCrop();
                    profileOptions.placeholder(R.drawable.ic_avatar_black);
                    Glide.with(getApplicationContext())
                            .load(dpUrl)
                            .apply(profileOptions)
                            .into(mDp);
                }
                if (dataSnapshot.hasChild(Constants.COVER_URL)) {
                    String coverUrl = dataSnapshot.child(Constants.COVER_URL).getValue().toString();
                    RequestOptions coverOptions = new RequestOptions();
                    coverOptions.fitCenter();
                    Glide.with(getApplicationContext())
                            .load(coverUrl)
                            .into(mCoverImage);
                }
                if (dataSnapshot.hasChild(Constants.USERNAME_PROFILE)) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    mName.setText(user.getDisplayName());
                }
                if (dataSnapshot.hasChild(Constants.BIO)) {
                    mBio.setText(dataSnapshot.child(Constants.BIO).getValue().toString());
                }
                if (dataSnapshot.hasChild(Constants.LOCATION)) {
                    mLocation.setText(dataSnapshot.child(Constants.LOCATION).getValue().toString());
                }
                if (dataSnapshot.hasChild(Constants.WORK)) {
                    mWork.setText(dataSnapshot.child(Constants.WORK).getValue().toString());
                }
                if (dataSnapshot.hasChild(Constants.EDUCATION)) {
                    mEducation.setText(dataSnapshot.child(Constants.EDUCATION).getValue().toString());
                }
                if (dataSnapshot.hasChild(Constants.SKILLS)) {
                    mSkills.setText(dataSnapshot.child(Constants.SKILLS).getValue().toString());
                }
                mProgress.dismiss();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Edit button handler.
        mDoneFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgress.setTitle("Please wait");
                mProgress.setMessage("We are updating your profile.");
                mProgress.setCancelable(false);
                mProgress.show();

                if (!TextUtils.isEmpty(mName.getText().toString())) {
                    UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder()
                            .setDisplayName(mName.getText().toString())
                            .build();
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    user.updateProfile(userProfileChangeRequest).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mUserDataRef.child(Constants.USERNAME_PROFILE).setValue(mName.getText().toString());
                        }
                    });
                }
                if (!TextUtils.isEmpty(mBio.getText().toString()))
                    mUserDataRef.child(Constants.BIO).setValue(mBio.getText().toString());
                if (!TextUtils.isEmpty(mLocation.getText().toString()))
                    mUserDataRef.child(Constants.LOCATION).setValue(mLocation.getText().toString());
                if (!TextUtils.isEmpty(mWork.getText().toString()))
                    mUserDataRef.child(Constants.WORK).setValue(mWork.getText().toString());
                if (!TextUtils.isEmpty(mEducation.getText().toString()))
                    mUserDataRef.child(Constants.EDUCATION).setValue(mEducation.getText().toString());
                if (!TextUtils.isEmpty(mSkills.getText().toString()))
                    mUserDataRef.child(Constants.SKILLS).setValue(mSkills.getText().toString());

                mProgress.dismiss();

                if (Build.VERSION.SDK_INT >= 21) {
                    Snackbar.make(findViewById(R.id.edit_profile_coo),
                            "Profile Details Updated.",
                            Snackbar.LENGTH_SHORT).show();
                } else
                    Toast.makeText(ProfileEdit.this, "Profile Details Updated.", Toast.LENGTH_SHORT).show();
            }
        });

        //DP click listener.
        mDp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDPPressed = true;
                String actions[] = new String[]{"View Image", "Change Image", "Remove Image"};
                AlertDialog.Builder builder = new AlertDialog.Builder(ProfileEdit.this);
                builder.setTitle("Choose action for profile")
                        .setItems(actions, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {

                                    case 0:
                                        //Showing the full quality image.
                                        final AlertDialog dialog2;
                                        AlertDialog.Builder builder1 = new AlertDialog.Builder(ProfileEdit.this);
                                        final View view = LayoutInflater.from(ProfileEdit.this)
                                                .inflate(R.layout.see_profile_img, null);
                                        builder1.setView(view)
                                                .setTitle("Profile Image");
                                        dialog2 = builder1.create();
                                        final ImageView showImage = (ImageView) view.findViewById(R.id.show_dp_img);
                                        dialog2.show();

                                        mUserDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                if(dataSnapshot.hasChild(Constants.DP_URL)){
                                                    RequestOptions requestOptions = new RequestOptions();
                                                    requestOptions.placeholder(getResources().getDrawable(R.drawable.ic_photo));
                                                    //Loading the image.
                                                    Glide.with(ProfileEdit.this)
                                                            .load(dataSnapshot.child(Constants.DP_URL).getValue().toString())
                                                            .apply(requestOptions)
                                                            .into(showImage);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });

                                        mDPPressed = false;
                                        break;
                                    case 1:
                                        //Choosing new image for uploading
                                        CropImage.activity()
                                                .setGuidelines(CropImageView.Guidelines.ON)
                                                .setAspectRatio(1, 1)
                                                .setAutoZoomEnabled(true)
                                                .start(ProfileEdit.this);
                                        break;
                                    case 2:
                                        //Removing the DP pic values.
                                        mUserDataRef.child(Constants.DP_THUMB_URL).removeValue();
                                        mUserDataRef.child(Constants.DP_URL).removeValue()
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        mDp.setImageDrawable(null);
                                                        mDp.setImageDrawable(getResources().getDrawable(R.drawable.ic_avatar_black));

                                                        if (Build.VERSION.SDK_INT >= 21) {
                                                            Snackbar.make(findViewById(R.id.edit_profile_coo),
                                                                    "Profile Picture removed.",
                                                                    Snackbar.LENGTH_SHORT).show();
                                                        } else
                                                            Toast.makeText(ProfileEdit.this, "Profile Picture removed.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                        mDPPressed = false;
                                        break;
                                }

                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        //Handling cover image click events.
        mCoverImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCoverPressed = true;
                String actions[] = new String[]{"View Image", "Change Image", "Remove Image"};
                AlertDialog.Builder builder = new AlertDialog.Builder(ProfileEdit.this);
                builder.setTitle("Choose action for cover")
                        .setItems(actions, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {

                                    case 0:
                                        //Showing the full quality image.
                                        final AlertDialog dialog2;
                                        AlertDialog.Builder builder1 = new AlertDialog.Builder(ProfileEdit.this);
                                        final View view = LayoutInflater.from(ProfileEdit.this)
                                                .inflate(R.layout.see_profile_img, null);
                                        builder1.setView(view)
                                                .setTitle("Cover Image");
                                        dialog2 = builder1.create();
                                        final ImageView showImage = (ImageView) view.findViewById(R.id.show_dp_img);
                                        dialog2.show();

                                        mUserDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                if(dataSnapshot.hasChild(Constants.COVER_URL)){
                                                    RequestOptions requestOptions = new RequestOptions();
                                                    requestOptions.placeholder(getResources().getDrawable(R.drawable.ic_photo));

                                                    Glide.with(ProfileEdit.this)
                                                            .load(dataSnapshot.child(Constants.COVER_URL).getValue().toString())
                                                            .apply(requestOptions)
                                                            .into(showImage);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });

                                        mCoverPressed = false;
                                        break;
                                    case 1:
                                        //Choosing the cover image for upload.
                                        CropImage.activity()
                                                .setGuidelines(CropImageView.Guidelines.ON)
                                                .setAspectRatio(2, 1)
                                                .setAutoZoomEnabled(true)
                                                .start(ProfileEdit.this);
                                        break;
                                    case 2:
                                        //Removing the thumb image values.
                                        mUserDataRef.child(Constants.COVER_THUMB_URL).removeValue();
                                        mUserDataRef.child(Constants.COVER_URL).removeValue()
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        mCoverImage.setImageDrawable(null);
                                                        mCoverImage.setImageDrawable(getResources().getDrawable(R.drawable.navigation_drawer_image));
                                                        if (Build.VERSION.SDK_INT >= 21) {
                                                            Snackbar.make(findViewById(R.id.edit_profile_coo),
                                                                    "Cover Picture removed.",
                                                                    Snackbar.LENGTH_SHORT).show();
                                                        } else
                                                            Toast.makeText(ProfileEdit.this, "Cover Picture removed.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                        mCoverPressed = false;
                                        break;
                                }

                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        //Got the chosen image.
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                //Displaying Msg.
                mProgress.setTitle("Please wait");
                mProgress.setMessage("Please wait we are processing your image.");
                mProgress.setCancelable(false);
                mProgress.show();

                //Creating a thumbnail with image compressor.
                //thumbReady will be true when done.
                Boolean thumbReady = false;
                final Uri actualImageUri = result.getUri();
                Uri tempUri = null;
                File actualUploadFile = new File(actualImageUri.getPath());
                final File compressedFile;
                try {
                    compressedFile = new Compressor(this)
                            .setQuality(20)
                            .setCompressFormat(Bitmap.CompressFormat.JPEG)
                            .compressToFile(actualUploadFile);
                    tempUri = Uri.fromFile(compressedFile);
                    thumbReady = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                final Uri compressedImageUri = tempUri;     //Compressed image Uri

                //If DP was pressed
                    //mDPPressed will be true.
                if (mDPPressed && thumbReady) {
                    mProgress.setMessage("Uploading your profile picture");
                    mProgress.show();
                    mProfileImagefRef.child(userId + ".jpg")
                            .putFile(actualImageUri)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot dpTaskSnapshot) {
                                    mUserDataRef.child(Constants.DP_URL).setValue(dpTaskSnapshot.getDownloadUrl().toString());
                                    mProgress.show();
                                    mProgress.setMessage("Almost done.");

                                    //Checking if image path is correct.
                                    if (compressedImageUri != null) {
                                        mProfileThumRef.child(userId + "-thumb.jpg")
                                                .putFile(compressedImageUri)
                                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                    @Override
                                                    public void onSuccess(UploadTask.TaskSnapshot thumbTaskSnapshot) {
                                                        mUserDataRef.child(Constants.DP_THUMB_URL).setValue(thumbTaskSnapshot.getDownloadUrl().toString());
                                                        mDPPressed = false;
                                                        mProgress.dismiss();
                                                        if (Build.VERSION.SDK_INT >= 21) {
                                                            Snackbar.make(findViewById(R.id.edit_profile_coo),
                                                                    "Profile Picture Updated.",
                                                                    Snackbar.LENGTH_SHORT).show();
                                                        } else
                                                            Toast.makeText(ProfileEdit.this, "Profile Picture Updated.", Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        mDPPressed = false;
                                                        mProgress.dismiss();
                                                    }
                                                });
                                    } else {
                                        if (Build.VERSION.SDK_INT >= 21) {
                                            Snackbar.make(findViewById(R.id.edit_profile_coo),
                                                    "Sorry some error occurred.",
                                                    Snackbar.LENGTH_LONG).show();
                                        } else
                                            Toast.makeText(ProfileEdit.this, "Sorry some error occurred.", Toast.LENGTH_LONG).show();
                                        mDPPressed = false; //needs to be false at the end.
                                        mProgress.dismiss();
                                    }
                                    //Loading the DP.
                                    RequestOptions profileOptions = new RequestOptions();
                                    profileOptions.circleCrop();
                                    Glide.with(ProfileEdit.this)
                                            .load(actualImageUri)
                                            .apply(profileOptions)
                                            .into(mDp);

                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    mDPPressed = false;
                                    mProgress.dismiss();
                                    if (Build.VERSION.SDK_INT >= 21) {
                                        Snackbar.make(findViewById(R.id.edit_profile_coo),
                                                "Profile Picture was not updated.",
                                                Snackbar.LENGTH_LONG).show();
                                    } else
                                        Toast.makeText(ProfileEdit.this, "Profile picture was not updated", Toast.LENGTH_LONG).show();
                                }
                            });
                    //If cover pic was clicked.
                        //mCoverPressed will be true.
                } else if (mCoverPressed && thumbReady) {
                    //Displaying msg.
                    mProgress.setMessage("Uploading your cover picture");
                    mProgress.show();
                    mCoverImageRef.child(userId + ".jpg")
                            .putFile(actualImageUri)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot coverTaskSnapshot) {
                                    mUserDataRef.child(Constants.COVER_URL).setValue(coverTaskSnapshot.getDownloadUrl().toString());
                                    mProgress.setMessage("Almost done");
                                    mProgress.show();
                                    //Checking if image uri is correct.
                                    if (compressedImageUri != null) {
                                        mCoverThumRef.child(userId + "-thumb.jpg")
                                                .putFile(compressedImageUri)
                                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                    @Override
                                                    public void onSuccess(UploadTask.TaskSnapshot thumbTaskSnapshot) {
                                                        mUserDataRef.child(Constants.COVER_THUMB_URL).setValue(thumbTaskSnapshot.getDownloadUrl().toString());
                                                        mCoverPressed = false;
                                                        mProgress.dismiss();
                                                        if (Build.VERSION.SDK_INT >= 21) {
                                                            Snackbar.make(findViewById(R.id.edit_profile_coo),
                                                                    "Cover Picture Updated.",
                                                                    Snackbar.LENGTH_SHORT).show();
                                                        } else
                                                            Toast.makeText(ProfileEdit.this, "Cover Picture Updated.", Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        mCoverPressed = false;
                                                        mProgress.dismiss();
                                                    }
                                                });
                                    } else {
                                        mCoverPressed = false;
                                        mProgress.dismiss();
                                        if (Build.VERSION.SDK_INT >= 21) {
                                            Snackbar.make(findViewById(R.id.edit_profile_coo),
                                                    "Sorry some error occurred.",
                                                    Snackbar.LENGTH_LONG).show();
                                        } else
                                            Toast.makeText(ProfileEdit.this, "Sorry some error occurred.", Toast.LENGTH_LONG).show();
                                    }
                                    Glide.with(ProfileEdit.this)
                                            .load(actualImageUri)
                                            .into(mCoverImage);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    mCoverPressed = false;
                                    mProgress.dismiss();
                                    if (Build.VERSION.SDK_INT >= 21) {
                                        Snackbar.make(findViewById(R.id.edit_profile_coo),
                                                "Cover Picture was not updated.",
                                                Snackbar.LENGTH_LONG).show();
                                    } else
                                        Toast.makeText(ProfileEdit.this, "Cover picture was not updated", Toast.LENGTH_LONG).show();
                                }
                            });

                //If there was a problem in image compression.
                } else {
                    mProgress.dismiss();
                    if (Build.VERSION.SDK_INT >= 21) {
                        Snackbar.make(findViewById(R.id.edit_profile_coo),
                                "Sorry some internal error occurred.",
                                Snackbar.LENGTH_LONG).show();
                    } else
                        Toast.makeText(ProfileEdit.this, "Sorry some internal error occurred.", Toast.LENGTH_LONG).show();
                }
                //Getting the saved intent data at the beg since there was a return back to UI.
                SharedPreferences preferences = this.getSharedPreferences(Constants.INTENT_DATA, MODE_PRIVATE);
                mLastActivity = preferences.getString(Constants.ACTIVITY, Constants.POST_ACTIVITY);
                mProfileId = preferences.getString(Constants.USERID, null);
            //Error in choosing.
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                if (Build.VERSION.SDK_INT >= 21) {
                    Snackbar.make(findViewById(R.id.edit_profile_coo),
                            error.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                } else
                    Toast.makeText(ProfileEdit.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

    }

    //If the hardware back button is pressed.
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        sendBackData();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int id = item.getItemId();
        switch (id) {
            // If the back toolbar back is pressed.
            case android.R.id.home:
                sendBackData();
                return true;
        }
        return false;
    }
    //Returning back data to caller activity.
    private void sendBackData() {
        Intent backData = new Intent();
        backData.putExtra(Constants.ACTIVITY, mLastActivity);
        backData.putExtra(Constants.USERID, mProfileId);
        setResult(RESULT_OK, backData);
        finish();
    }
}
