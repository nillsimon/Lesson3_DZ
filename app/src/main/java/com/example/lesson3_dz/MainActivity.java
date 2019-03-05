package com.example.lesson3_dz;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
public class MainActivity extends AppCompatActivity {

    final String TAG = "MainActivity";
    static final int PICK_REQUEST = 1;
    String filesLocation;
    final String picIn = "pic.jpg";
    final String picOut = "pic.png";

    @BindView(R.id.imageView) ImageView imageView;
    @BindView(R.id.loadPBar) ProgressBar loadPBar;
    @BindView(R.id.convertPBar) ProgressBar convertPBar;
    @BindView(R.id.loadButton) Button loadBtn;
    @BindView(R.id.convertButton) Button convertBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        filesLocation = getApplicationContext().getFilesDir().toString();

        loadPBar.setVisibility(View.GONE);
        convertPBar.setVisibility(View.GONE);

         loadBtn.setOnClickListener(v -> {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, PICK_REQUEST);
            convertBtn.setVisibility(View.GONE);
        });

         convertBtn.setVisibility(View.GONE);

            convertBtn.setOnClickListener(v -> {
            convertBtn.setVisibility(View.INVISIBLE);
            convertPBar.setVisibility(View.VISIBLE);
            Observable<String> observable = Observable.create(emitter -> {
                try {
                    File file = new File(filesLocation, picIn);
                    File filePNG = new File(filesLocation, picOut);
                    if (file.exists()) {
                        Log.d(TAG, "файл существует");
                        // конвертировать
                        Log.d(TAG, "converting...");
                        OutputStream outputStream = new FileOutputStream(filePNG);
                        Bitmap bitmap =
                                BitmapFactory.decodeFile(file.getAbsolutePath());
                        bitmap.compress(Bitmap.CompressFormat.PNG,
                                90, outputStream);
                        outputStream.flush();
                        outputStream.close();
                        Log.d(TAG, "...converted");
                    }
                    if (filePNG.exists()) {
                        Log.d(TAG, "file PNG exists");
                        emitter.onNext(picOut);
                    }

                } catch (Exception e) {
                    emitter.onError(e);
                }
            });

            observable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<String>() {
                        @Override
                        public void onNext(String s) {
                            Log.d(TAG, "--- filename " + s);
                            convertBtn.setVisibility(View.VISIBLE);
                            convertPBar.setVisibility(View.GONE);
                            if (!s.equals(""))
                                convertBtn.setText("Конвертирование выполнено");
                            else
                                convertBtn.setText("Конвертирование не выполнено");
                        }

                        @Override
                        public void onComplete() {
                        }

                        @Override
                        public void onError(Throwable t) {
                            Log.d(TAG, "Error.", t);
                            convertBtn.setText("Конвертирование не выполнено");
                        }

                        @Override
                        public void onSubscribe(Disposable d) {

                        }
                    });

        });
    }

    public void saveBitmapFile(Bitmap bitmap) {
        loadBtn.setVisibility(View.INVISIBLE);
        convertBtn.setVisibility(View.INVISIBLE);
        loadPBar.setVisibility(View.VISIBLE);
        Observable<String> observable = Observable.create(emitter -> {
            try {
                File file = new File(filesLocation, picIn);
                if (file.exists()) {
                    boolean res = file.delete();
                    if (res)
                        file = new File(filesLocation, picIn);
                }
                try {
                    Log.d(TAG, "--- loading... ");
                    OutputStream outStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, outStream);
                    outStream.flush();
                    outStream.close();
                    Log.d(TAG, "--- ...loaded " + picIn);
                } catch (Exception e) {
                    e.printStackTrace();
                    convertBtn.setText("Конвертирование не выполнено");
                }
                emitter.onNext(picIn);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });

        observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String s) {
                        Log.d(TAG, "--- filename " + s);
                        loadPBar.setVisibility(View.GONE);
                        loadBtn.setVisibility(View.VISIBLE);
                        convertBtn.setVisibility(View.VISIBLE);
                        Toast.makeText(MainActivity.this, "Файл сохранен", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.d(TAG, "Error.", t);
                    }

                    @Override
                    public void onSubscribe(Disposable d) {

                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        Bitmap bitmap = null;

        switch (requestCode) {
            case PICK_REQUEST:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // сохранить bitmap в jpg
                    saveBitmapFile(bitmap);

                    Picasso.with(this)
                            .load(selectedImage)
                            .fit()
                            .centerCrop()
                            .into(imageView);


             }
        }
    }
}