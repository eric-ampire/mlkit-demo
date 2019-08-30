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

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CAPTURE = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 4;
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

    public void processDetection(View view) {

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                String[] permission = {Manifest.permission.CAMERA};
                requestPermissions(permission, 1);
            } else {
                startCamera();
            }

        } else {
            startCamera();
        }
    }

    private void startCamera() {
        Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (imageIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(imageIntent, REQUEST_CODE_CAPTURE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, String.valueOf(grantResults[0]));
                startCamera();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CAPTURE && data != null) {
                Uri imageUri = data.getData();
                if (imageUri != null) {

                    Log.i(TAG, "onActivityResult");
                    imageBitmap = getBitmapFromUri(getPathFromURI(imageUri));
                    previewImage.setImageURI(imageUri);
                }
            }
        }
    }

    private Bitmap getBitmapFromUri(String imageUri) {
        try {
            Log.i(TAG, "getBitmapFromUri");
            return decodeSampledBitmapFromFile(imageUri, 200, 200);
        } catch (Exception e) {
            return null;
        }
    }

    public String getPathFromURI(Uri contentUri)
    {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = this.getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor == null) return "emp";

        try
        {
            Log.i(TAG, "getPathFromURI");

            int column_index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        catch (Exception e)
        {
            return contentUri.getPath();
        }
        finally {
            cursor.close();
        }
    }


    public static Bitmap decodeSampledBitmapFromFile(String imagePath,
                                                     int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        //return Bitmap.createScaledBitmap(bt, reqWidth, reqHeight, false);
        return BitmapFactory.decodeFile(imagePath, options);
    }


    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

}
