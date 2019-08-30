package com.ericampire.mobile.mlkit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements PermissionListener {

    private static final int REQUEST_CODE_CAPTURE = 2;
    private static final String TAG = "MainActivity";
    private ImageView previewImage;
    private ProgressBar progressBar;
    private LinearLayout buttonLayout;
    private Bitmap imageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewImage = findViewById(R.id.ivPreview);
        progressBar = findViewById(R.id.pbLoading);
        buttonLayout = findViewById(R.id.lyButton);
    }

    public void processDetection() {

        Log.i(TAG, "processDetection");
        if (imageBitmap == null) return;

        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();

        // Start Progression
        progressBar.setVisibility(View.VISIBLE);
        buttonLayout.setEnabled(false);

        Task<FirebaseVisionText> result = detector.processImage(firebaseVisionImage);
        result.addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                TextView resultTextView = findViewById(R.id.tvResult);

                // Getting ImageText
                resultTextView.setText(firebaseVisionText.getText());

                progressBar.setVisibility(View.INVISIBLE);
                buttonLayout.setEnabled(true);

                // Showing Success Message
                Toast.makeText(getBaseContext(), "Success", Toast.LENGTH_LONG).show();
            }
        });

        result.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.INVISIBLE);
                buttonLayout.setEnabled(true);
                Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void pickImage(View view) {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(this)
                .check();
    }

    private void startCamera() {
        Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (imageIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(imageIntent, REQUEST_CODE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CAPTURE && resultCode == RESULT_OK && data != null) {
            imageBitmap = (Bitmap) data.getExtras().get("data");
            previewImage.setImageBitmap(imageBitmap);
            processDetection();
        }
    }

    @Override
    public void onPermissionGranted(PermissionGrantedResponse response) {
        startCamera();
    }

    @Override
    public void onPermissionDenied(PermissionDeniedResponse response) {
        Toast.makeText(this, "L'application n'a eu le droit d'utiliser la camera", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

    }
}
