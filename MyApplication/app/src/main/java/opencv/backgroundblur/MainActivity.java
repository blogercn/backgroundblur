package opencv.backgroundblur;

/**
 * Created by jiazhiguo on 2017/6/8.
 */

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;


public class MainActivity extends Activity {

    static final int REQUEST_OPEN_IMAGE = 1;

    String mCurrentPhotoPath;
    Bitmap mBitmap;
    ImageView mImageView;
    int touchCount = 0;
    Point tl;
    Point br;
    boolean targetChose = false;
    ProgressDialog dlg;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    //Log.i(TAG, "OpenCV loaded successfully");

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = (ImageView) findViewById(R.id.imgDisplay);
        dlg = new ProgressDialog(this);
        tl = new Point();
        br = new Point();
        // if (!OpenCVLoader.initDebug()) {
        // Handle initialization error
        //}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    int scaleFactor = 1;
    private void setPic() {
        int targetW = 720;//mImageView.getWidth();
        int targetH = 1128;//mImageView.getHeight();
        Log.i(">>>>>", "targetW="+targetW);
        Log.i(">>>>>", "targetH=" + targetH);
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        Log.i(">>>>>", "photoW="+photoW);
        Log.i(">>>>>", "photoH=" + photoH);

        scaleFactor = Math.max(photoW / targetW, photoH / targetH)+1;
        Log.i(">>>>>", "photoW / targetW="+(photoW / targetW));
        Log.i(">>>>>", "photoH / targetH="+(photoH / targetH));

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        mBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageView.setImageBitmap(mBitmap);
        Log.i(">>>>>", "mBitmap.getWidth()="+mBitmap.getWidth());
        Log.i(">>>>>", "mBitmap.getHeight()=" + mBitmap.getHeight());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_OPEN_IMAGE:
                if (resultCode == RESULT_OK) {
                    Uri imgUri = data.getData();
                    String[] filePathColumn = { MediaStore.Images.Media.DATA };

                    Cursor cursor = getContentResolver().query(imgUri, filePathColumn,
                            null, null, null);
                    cursor.moveToFirst();

                    int colIndex = cursor.getColumnIndex(filePathColumn[0]);
                    mCurrentPhotoPath = cursor.getString(colIndex);
                    cursor.close();
                    setPic();
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_open_img:
                Intent getPictureIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getPictureIntent.setType("image/*");
                Intent pickPictureIntent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                Intent chooserIntent = Intent.createChooser(getPictureIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {
                        pickPictureIntent
                });
                startActivityForResult(chooserIntent, REQUEST_OPEN_IMAGE);
                return true;
            case R.id.action_choose_target:

                if (mCurrentPhotoPath != null)
                    targetChose = false;
                mImageView.setOnTouchListener(new View.OnTouchListener() {

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        int cx = (mImageView.getWidth()-mBitmap.getWidth())/2;
                        int cy = (mImageView.getHeight()-mBitmap.getHeight())/2;
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            if (touchCount == 0) {
                                tl.x = event.getX();//300;//
                                tl.y = event.getY();//300;//
                                touchCount++;
                            }
                            else if (touchCount == 1) {
                                br.x = event.getX();//1100;//
                                br.y = event.getY();//1200;//

                                Paint rectPaint = new Paint();
                                rectPaint.setARGB(255, 255, 0, 0);
                                rectPaint.setStyle(Paint.Style.STROKE);
                                rectPaint.setStrokeWidth(3);
                                Bitmap tmpBm = Bitmap.createBitmap(mBitmap.getWidth(),
                                        mBitmap.getHeight(), Bitmap.Config.RGB_565);
                                Canvas tmpCanvas = new Canvas(tmpBm);

                                tmpCanvas.drawBitmap(mBitmap, 0, 0, null);
                                tmpCanvas.drawRect(new RectF((float) tl.x, (float) tl.y, (float) br.x, (float) br.y),
                                        rectPaint);
                                mImageView.setImageDrawable(new BitmapDrawable(getResources(), tmpBm));

                                targetChose = true;
                                touchCount = 0;
                                mImageView.setOnTouchListener(null);
                            }
                        }

                        return true;
                    }
                });

                return true;
            case R.id.action_cut_image:
                if (mCurrentPhotoPath != null && targetChose) {
                    new ProcessImageTask().execute();
                    targetChose = false;
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ProcessImageTask extends AsyncTask<Integer, Integer, Integer> {
        Mat img;
        Mat foreground;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dlg.setMessage("Processing Image...");
            dlg.setCancelable(false);
            dlg.setIndeterminate(true);
            dlg.show();
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            //Mat img = new Mat(mBitmap.getHeight(), mBitmap.getHeight(), CvType.CV_8UC3);
            //Utils.bitmapToMat(mBitmap, img);
            long ll = System.currentTimeMillis();
            Log.i(">>>>>", "start="+ll);
            img = Imgcodecs.imread(mCurrentPhotoPath);
            Imgproc.resize(img, img, new Size(img.cols()/scaleFactor, img.rows()/scaleFactor));
            Log.i(">>>>>", "11111=" + System.currentTimeMillis()+"@@@@@"+(System.currentTimeMillis()-ll));
            Mat background = new Mat(img.size(), CvType.CV_8UC3,
                    new Scalar(255, 255, 255));

            Mat firstMask = new Mat();
            Mat bgModel = new Mat();
            Mat fgModel = new Mat();
            Mat mask;
            Mat source = new Mat(1, 1, CvType.CV_8U, new Scalar(Imgproc.GC_PR_FGD));
            Mat dst = new Mat();
            Rect rect = new Rect(tl, br);
            Log.i(">>>>>", "22222="+System.currentTimeMillis()+"@@@@@"+(System.currentTimeMillis()-ll));
            Imgproc.grabCut(img, firstMask, rect, bgModel, fgModel,
                    1, Imgproc.GC_INIT_WITH_RECT);
            Log.i(">>>>>", "33333=" + System.currentTimeMillis() + "@@@@@" + (System.currentTimeMillis() - ll));
            Core.compare(firstMask, source, firstMask, Core.CMP_EQ);
            Log.i(">>>>>", "44444=" + System.currentTimeMillis() + "@@@@@" + (System.currentTimeMillis() - ll));
            foreground = new Mat(img.size(), CvType.CV_8UC3,
                    new Scalar(255, 255, 255));
            /////
            img.copyTo(foreground);
            Imgproc.blur(foreground, foreground, new Size(20, 20));
            Log.i(">>>>>", "55555=" + System.currentTimeMillis()+"@@@@@"+(System.currentTimeMillis()-ll));
            /////
            img.copyTo(foreground, firstMask);
            Log.i(">>>>>", "66666=" + System.currentTimeMillis()+"@@@@@"+(System.currentTimeMillis()-ll));

            firstMask.release();
            source.release();
            bgModel.release();
            fgModel.release();

            Imgcodecs.imwrite(mCurrentPhotoPath + ".png", foreground);

            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);

            Bitmap jpg = BitmapFactory
                    .decodeFile(mCurrentPhotoPath + ".png");

            mImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            mImageView.setAdjustViewBounds(true);
            mImageView.setPadding(2, 2, 2, 2);
            mImageView.setImageBitmap(jpg);
            mImageView.invalidate();


            dlg.dismiss();
        }
    }
}
