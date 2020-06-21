package ai.fritz.camera;

        import android.graphics.Bitmap;
        import android.graphics.Canvas;
        import android.graphics.Color;
        import android.graphics.Paint;
        import android.graphics.Point;
        import android.graphics.PointF;
        import android.graphics.PorterDuff;
        import android.graphics.PorterDuffXfermode;
        import android.graphics.RectF;
        import android.media.Image;
        import android.media.ImageReader;
        import android.os.Bundle;
        import android.util.DisplayMetrics;
        import android.util.Log;
        import android.util.Pair;
        import android.util.Size;
        import android.view.SurfaceView;
        import android.view.View;
        import android.widget.Button;
        import android.widget.ProgressBar;
        import android.widget.RelativeLayout;

        import java.util.Objects;

        import org.json.JSONArray;

        import java.util.List;
        import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.Object;

        import ai.fritz.core.Fritz;
        import ai.fritz.poseestimationdemo.R;
        import ai.fritz.vision.FritzVision;
        import ai.fritz.vision.FritzVisionImage;
        import ai.fritz.vision.FritzVisionOrientation;
        import ai.fritz.vision.ImageOrientation;
        import ai.fritz.vision.base.DrawingUtils;
        import ai.fritz.vision.poseestimation.FritzVisionPosePredictor;
        import ai.fritz.vision.poseestimation.FritzVisionPoseResult;
        import ai.fritz.vision.poseestimation.HumanSkeleton;
        import ai.fritz.vision.poseestimation.Keypoint;
        import ai.fritz.vision.poseestimation.Pose;
        import ai.fritz.vision.poseestimation.PoseOnDeviceModel;
        import ai.fritz.vision.poseestimation.PoseDecoder;
        import ai.fritz.vision.poseestimation.Skeleton;
        import ai.fritz.camera.FakeSkeleton;


public class MainActivity extends BaseCameraActivity implements ImageReader.OnImageAvailableListener {

    private static final Size DESIRED_PREVIEW_SIZE = new Size(1280, 960);

    private AtomicBoolean isComputing = new AtomicBoolean(false);
    private AtomicBoolean shouldSample = new AtomicBoolean(true);
    private ImageOrientation orientation;
    public int offset = 150;
    public int reps = 0;
    public int drawFlag = 0; // initially draw the gray
    public int drawFlag2 = 0;
    public int endFlag = 0;
    public int endFlag2 = 0;

    FritzVisionPoseResult poseResult;
    FritzVisionPosePredictor predictor;
    FritzVisionImage visionImage;

    // Preview Frame
    RelativeLayout previewFrame;
    Button snapshotButton;
    ProgressBar snapshotProcessingSpinner;

    // Snapshot Frame
    RelativeLayout snapshotFrame;
    OverlayView snapshotOverlay;
    Button closeButton;
    Button recordButton;
    ProgressBar recordSpinner;

    String disease= new String();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        disease = getIntent().getStringExtra("key");

        Fritz.configure(this);
        // The code below loads a custom trained pose estimation model and creates a predictor that will be used to identify poses in live video.
        // Custom pose estimation models can be trained with the Fritz AI platform. To use a pre-trained pose estimation model,
        // see the FritzAIStudio demo in this repo.
        PoseOnDeviceModel poseEstimationOnDeviceModel = PoseOnDeviceModel.buildFromModelConfigFile("pose_recording_model.json", new HumanSkeleton());
        predictor = FritzVision.PoseEstimation.getPredictor(poseEstimationOnDeviceModel);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.main_camera;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onPreviewSizeChosen(final Size previewSize, final Size cameraViewSize, final int rotation) {
        orientation = FritzVisionOrientation.getImageOrientationFromCamera(this, cameraId);


        // Preview View
        previewFrame = findViewById(R.id.preview_frame);
        snapshotProcessingSpinner = findViewById(R.id.snapshot_spinner);
        snapshotButton = findViewById(R.id.take_picture_btn);
        snapshotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!shouldSample.compareAndSet(true, false)) {
                    return;
                }

                runInBackground(
                        () -> {
                            showSpinner();
                            snapshotOverlay.postInvalidate();
                            switchToSnapshotView();
                            hideSpinner();
                        });
            }
        });

        /*
        switch (1) {
            case 1:
                shoulderpress();
                //case 2:
                //    legSideRaise();
                break;
            case 3:
                neckRoll();
                break;
        }
         */
        switch (disease) {
            case "1":
                shoulderpress();
            case "2":
                 //legSideRaise();
                break;
            case "3":
                neckRoll();
                break;

        }

        // Snapshot View
        snapshotFrame = findViewById(R.id.snapshot_frame);
        snapshotOverlay = findViewById(R.id.snapshot_view);
        snapshotOverlay.setCallback(
                canvas -> {
                    if (poseResult != null) {
                        Bitmap bitmap = visionImage.overlaySkeletons(poseResult.getPoses());
                        canvas.drawBitmap(bitmap, null, new RectF(0, 0, cameraViewSize.getWidth(), cameraViewSize.getHeight()), null);
                    }
                });

        recordSpinner = findViewById(R.id.record_spinner);
        recordButton = findViewById(R.id.record_prediction_btn);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordSpinner.setVisibility(View.VISIBLE);
                // To record predictions and send data back to Fritz AI via the Data Collection System, use the predictors's record method.
                // In addition to the input image, predicted model results can be collected as well as user-modified annotations.
                // This allows developers to both gather data on model performance and have users collect additional ground truth data for future model retraining.
                // Note, the Data Collection System is only available on paid plans.
                predictor.record(visionImage, poseResult, null, () -> {
                    switchPreviewView();
                    return null;
                }, () -> {
                    switchPreviewView();
                    return null;
                });
            }
        });
        closeButton = findViewById(R.id.close_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchPreviewView();
            }
        });

    }

    private void switchToSnapshotView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                previewFrame.setVisibility(View.GONE);
                snapshotFrame.setVisibility(View.VISIBLE);
            }
        });
    }

    private void switchPreviewView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recordSpinner.setVisibility(View.GONE);
                snapshotFrame.setVisibility(View.GONE);
                previewFrame.setVisibility(View.VISIBLE);
                shouldSample.set(true);
            }
        });
    }

    private void showSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                snapshotProcessingSpinner.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                snapshotProcessingSpinner.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = reader.acquireLatestImage();

        if (image == null) {
            return;
        }

        if (!shouldSample.get()) {
            image.close();
            return;
        }

        if (!isComputing.compareAndSet(false, true)) {
            image.close();
            return;
        }

        visionImage = FritzVisionImage.fromMediaImage(image, orientation);
        image.close();

        runInBackground(() -> {
            poseResult = predictor.predict(visionImage);
            requestRender();
        });
    }

    // Shoulder Press function (High range of motion required)
    // This function draws a shoulder press on the canvas for the user to complete reps.
    // First step is to get into position with arms at 90 degrees.
    // Second step is to lift arms.
    public void shoulderpress() {
        Paint paint = new Paint();
        Paint paint1 = new Paint();
        //paint1.setColor(android.R.color.green);
        paint1.setTextSize(75);

        setCallback(canvas -> {
            if (poseResult != null) {

                if (reps < 3) {
                    canvas.drawText("Shoulder Press", 50, 200, paint1);
                    for (Pose pose : poseResult.getPoses()) {
                        // draw the skeleton, do not delete lol
                        // pose.draw(canvas);


                        paint.setColor(Color.BLACK);
                        paint.setTextSize(100);
                        canvas.drawText("Reps : " + String.valueOf(reps), 50, 100, paint);

                        // Get height and width of screen
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                        int height = displayMetrics.heightPixels; // returns a float representing a pixel value
                        int width = displayMetrics.widthPixels;  //
                        Size Screen = new Size(width, height);


                        // Retrieve the array of keypoints
                        Keypoint[] example_keypoints = pose.getKeypoints();

                        // Lets play around with two keypoints and try to draw from one, to the other
                        Keypoint leftShoulder = example_keypoints[5].scaled(Screen);
                        Keypoint leftElbow = example_keypoints[7].scaled(Screen); // left elbow keypoint
                        Keypoint leftWrist = example_keypoints[9].scaled(Screen); // left wrist keypoint

                        Keypoint rightShoulder = example_keypoints[6].scaled(Screen);

                        PointF leftShoulderPosition = leftShoulder.getPosition();
                        PointF rightShoulderPosition = rightShoulder.getPosition();

                        float posShoulder_x = leftShoulderPosition.x; //  x position in PIXELS
                        float posShoulder_y = leftShoulderPosition.y; //  y position in PIXELS

                        float posRightShoulder_x = rightShoulderPosition.x; //  x position in PIXELS
                        float posRightShoulder_y = rightShoulderPosition.y; //  y position in PIXELS

                        // Package the desired positions referenced from shoulder.
                        PointF desiredElbowPostion = new PointF(posShoulder_x - offset, posShoulder_y);
                        PointF desiredWristPosition = new PointF(posShoulder_x + offset, posShoulder_y - offset);
                        PointF desiredWristPosition2 = new PointF(posShoulder_x + (float) 0.75 * offset, posShoulder_y - (float) 2.5 * offset);

                        float leftShoudlerDistance = leftElbow.calculateSquaredDistanceFromCoordinates(desiredElbowPostion);
                        float leftWristDistance = leftWrist.calculateSquaredDistanceFromCoordinates(desiredWristPosition);
                        float endLeftWristDistance = leftWrist.calculateSquaredDistanceFromCoordinates(desiredWristPosition2);

                        //String distance = String.valueOf(distanceFromDesired);
                        //Log.d("THIS IS THE DISTANCE", distance);
                        System.out.println(leftWristDistance);

                        if ((leftWristDistance <= 20000) && (endFlag2 == 0)) {
                            drawFlag = 1;
                            endFlag = 1;

                            if (drawFlag == 1) {
                                canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x + offset, posShoulder_y, DrawUtils2.DEFAULT_PAINT); // DEFAULT_PAINT
                                canvas.drawLine(posShoulder_x + offset, posShoulder_y, posShoulder_x + offset, posShoulder_y - offset, DrawUtils2.DEFAULT_PAINT);

                                canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x - offset, posRightShoulder_y, DrawUtils2.DEFAULT_PAINT); // DEFAULT_PAINT
                                canvas.drawLine(posRightShoulder_x - offset, posRightShoulder_y, posRightShoulder_x - offset, posRightShoulder_y - offset, DrawUtils2.DEFAULT_PAINT);

                                // OTHER LEFT draw the other pose in gray
                                canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x + (float) 0.75 * offset, posShoulder_y - (float) 1.5 * offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                                canvas.drawLine(posShoulder_x + (float) 0.75 * offset, posShoulder_y - (float) 1.5 * offset, posShoulder_x + (float) 0.75 * offset, posShoulder_y - (float) 3 * offset, DrawUtils2.GRAY_PAINT);
                                // OTHER RIGHT
                                canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x - (float) 0.75 * offset, posRightShoulder_y - (float) 1.5 * offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                                canvas.drawLine(posRightShoulder_x - (float) 0.75 * offset, posRightShoulder_y - (float) 1.5 * offset, posRightShoulder_x - (float) 0.75 * offset, posRightShoulder_y - (float) 3 * offset, DrawUtils2.GRAY_PAINT);


                                drawFlag2 = 1; // enabled coloring other pose green
                                endFlag2 = 1;
                            }


                        }

                        if ((endFlag == 1)) {
                            //drawFlag2 = 1;

                            if (drawFlag2 == 1 && (endLeftWristDistance <= 20000)) {
                                canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x + (float) 0.75 * offset, posShoulder_y - (float) 1.5 * offset, DrawUtils2.DEFAULT_PAINT); // DEFAULT_PAINT
                                canvas.drawLine(posShoulder_x + (float) 0.75 * offset, posShoulder_y - (float) 1.5 * offset, posShoulder_x + (float) 0.75 * offset, posShoulder_y - (float) 3 * offset, DrawUtils2.DEFAULT_PAINT);

                                canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x - (float) 0.75 * offset, posRightShoulder_y - (float) 1.5 * offset, DrawUtils2.DEFAULT_PAINT); // DEFAULT_PAINT
                                canvas.drawLine(posRightShoulder_x - (float) 0.75 * offset, posRightShoulder_y - (float) 1.5 * offset, posRightShoulder_x - (float) 0.75 * offset, posRightShoulder_y - (float) 3 * offset, DrawUtils2.DEFAULT_PAINT);

                                endFlag2 = 0;
                                drawFlag2 = 0;
                                drawFlag = 0; // initially draw the gray
                                endFlag = 0;
                                reps += 1;
                            } else {
                                //canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x + (float) 0.5*offset, posShoulder_y - (float) 1.5*offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                                //canvas.drawLine(posShoulder_x + (float) 0.5*offset, posShoulder_y - (float) 1.5*offset, posShoulder_x - offset, posShoulder_y - (float) 3.25*offset, DrawUtils2.GRAY_PAINT);
                                canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x + (float) 0.75 * offset, posShoulder_y - (float) 1.5 * offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                                canvas.drawLine(posShoulder_x + (float) 0.75 * offset, posShoulder_y - (float) 1.5 * offset, posShoulder_x + (float) 0.75 * offset, posShoulder_y - (float) 3 * offset, DrawUtils2.GRAY_PAINT);

                                canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x - (float) 0.75 * offset, posRightShoulder_y - (float) 1.5 * offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                                canvas.drawLine(posRightShoulder_x - (float) 0.75 * offset, posRightShoulder_y - (float) 1.5 * offset, posRightShoulder_x - (float) 0.75 * offset, posRightShoulder_y - (float) 3 * offset, DrawUtils2.GRAY_PAINT);
                            }
                        }


                        if (endFlag == 1 && drawFlag2 == 1) {
                            canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x + (float) 0.75 * offset, posShoulder_y - (float) 1.5 * offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                            canvas.drawLine(posShoulder_x + (float) 0.75 * offset, posShoulder_y - (float) 1.5 * offset, posShoulder_x + (float) 0.75 * offset, posShoulder_y - (float) 3 * offset, DrawUtils2.GRAY_PAINT);
                            canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x - (float) 0.75 * offset, posRightShoulder_y - (float) 1.5 * offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                            canvas.drawLine(posRightShoulder_x - (float) 0.75 * offset, posRightShoulder_y - (float) 1.5 * offset, posRightShoulder_x - (float) 0.75 * offset, posRightShoulder_y - (float) 3 * offset, DrawUtils2.GRAY_PAINT);
                        }

                        if (drawFlag == 0) {
                            // Draw from left shoulder guide for user in green. This is where they start
                            canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x + offset, posShoulder_y, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                            canvas.drawLine(posShoulder_x + offset, posShoulder_y, posShoulder_x + offset, posShoulder_y - offset, DrawUtils2.GRAY_PAINT);

                            canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x - offset, posRightShoulder_y, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                            canvas.drawLine(posRightShoulder_x - offset, posRightShoulder_y, posRightShoulder_x - offset, posRightShoulder_y - offset, DrawUtils2.GRAY_PAINT);


                        }
                    }
                } else {
                    //canvas.drawText("Side Arm Raise", 50, 200, paint);
                    sideArmRaise();
                }

            }
            isComputing.set(false);
        });
    }


    public void sideArmRaise() {
        offset = 200;
        reps = 0;
        drawFlag = 0; // initially draw the gray
        drawFlag2 = 0;
        endFlag = 0;
        endFlag2 = 0;

        Paint paint = new Paint();
        Paint paint1 = new Paint();
        //paint1.setColor(android.R.color.green);
        paint1.setTextSize(75);

        setCallback(canvas -> {
            if (poseResult != null) {


                if (reps < 3) {
                    for (Pose pose : poseResult.getPoses()) {
                        // draw the skeleton, do not delete lol
                        pose.draw(canvas);


                        paint.setColor(Color.BLACK);
                        paint.setTextSize(100);
                        canvas.drawText("Reps : " + String.valueOf(reps), 50, 100, paint);
                        canvas.drawText("Side Arm Raise", 50, 200, paint);

                        // Get height and width of screen
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                        int height = displayMetrics.heightPixels; // returns a float representing a pixel value
                        int width = displayMetrics.widthPixels;  //
                        Size Screen = new Size(width, height);


                        // Retrieve the array of keypoints
                        Keypoint[] example_keypoints = pose.getKeypoints();

                        // Lets play around with two keypoints and try to draw from one, to the other
                        Keypoint leftShoulder = example_keypoints[5].scaled(Screen);
                        Keypoint leftWrist = example_keypoints[9].scaled(Screen); // left wrist keypoint
                        Keypoint rightShoulder = example_keypoints[6].scaled(Screen);

                        PointF leftShoulderPosition = leftShoulder.getPosition();
                        PointF rightShoulderPosition = rightShoulder.getPosition();

                        float posShoulder_x = leftShoulderPosition.x; //  x position in PIXELS
                        float posShoulder_y = leftShoulderPosition.y; //  y position in PIXELS

                        float posRightShoulder_x = rightShoulderPosition.x; //  x position in PIXELS
                        float posRightShoulder_y = rightShoulderPosition.y; //  y position in PIXELS

                        // Package the desired positions referenced from shoulder.
                        PointF desiredWristPosition = new PointF(posShoulder_x, posShoulder_y + (float) 2.5 * offset);
                        PointF desiredWristPosition2 = new PointF(posShoulder_x + (float) 2.15 * offset, posShoulder_y);

                        //float leftShoudlerDistance = leftElbow.calculateSquaredDistanceFromCoordinates(desiredElbowPostion);
                        float leftWristDistance = leftWrist.calculateSquaredDistanceFromCoordinates(desiredWristPosition);
                        float endLeftWristDistance = leftWrist.calculateSquaredDistanceFromCoordinates(desiredWristPosition2);

                        //String distance = String.valueOf(distanceFromDesired);
                        //Log.d("THIS IS THE DISTANCE", distance);
                        System.out.println(leftWristDistance);

                        if ((leftWristDistance <= 20000) && (endFlag2 == 0)) {
                            drawFlag = 1;
                            endFlag = 1;

                            if (drawFlag == 1) {
                                canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x, posShoulder_y + (float) 2.5 * offset, DrawUtils2.DEFAULT_PAINT); // DEFAULT_PAINT
                                canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x, posRightShoulder_y + (float) 2.5 * offset, DrawUtils2.DEFAULT_PAINT); // DEFAULT_PAINT


                                // OTHER LEFT draw the other pose in gray
                                canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x + (float) 2.15 * offset, posShoulder_y, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                                canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x - (float) 2.15 * offset, posRightShoulder_y, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT

                                drawFlag2 = 1; // enabled coloring other pose green
                                endFlag2 = 1;
                            }


                        }

                        if ((endFlag == 1)) {
                            //drawFlag2 = 1;

                            if (drawFlag2 == 1 && (endLeftWristDistance <= 20000)) {
                                canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x + (float) 2.15 * offset, posShoulder_y, DrawUtils2.DEFAULT_PAINT);
                                canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x - (float) 2.15 * offset, posRightShoulder_y, DrawUtils2.DEFAULT_PAINT);

                                endFlag2 = 0;
                                drawFlag2 = 0;
                                drawFlag = 0; // initially draw the gray
                                endFlag = 0;
                                reps += 1;
                            } else {
                                canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x + (float) 2.15 * offset, posShoulder_y, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                                canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x - (float) 2.15 * offset, posRightShoulder_y, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT

                            }
                        }


                        if (endFlag == 1 && drawFlag2 == 1) {
                            canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x + (float) 2.15 * offset, posShoulder_y, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                            canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x - (float) 2.15 * offset, posRightShoulder_y, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                        }

                        if (drawFlag == 0) {
                            // Draw from left shoulder guide for user in green. This is where they start
                            canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x, posShoulder_y + (float) 2.5 * offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                            canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x, posRightShoulder_y + (float) 2.5 * offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT


                        }
                    }
                } else {
                    //canvas.drawText("Flap Arms", 50, 200, paint1);
                    flapArms();
                }

            }
            isComputing.set(false);
        });
    }


    public void flapArms() {
        offset = 200;
        reps = 0;
        drawFlag = 0; // initially draw the gray
        drawFlag2 = 0;
        endFlag = 0;
        endFlag2 = 0;

        setCallback(canvas -> {
            if (poseResult != null) {
                Paint paint = new Paint();
                Paint paint1 = new Paint();
                paint1.setTextSize(75);
                if (reps < 3) {
                    for (Pose pose : poseResult.getPoses()) {
                        // draw the skeleton, do not delete lol
                        pose.draw(canvas);

                        paint.setColor(Color.BLACK);
                        paint.setTextSize(100);
                        canvas.drawText("Reps. : " + String.valueOf(reps), 50, 100, paint);
                        canvas.drawText("Flap Arms", 50, 200, paint1);

                        // Get height and width of screen
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                        int height = displayMetrics.heightPixels; // returns a float representing a pixel value
                        int width = displayMetrics.widthPixels;  //
                        Size Screen = new Size(width, height);


                        // Retrieve the array of keypoints
                        Keypoint[] example_keypoints = pose.getKeypoints();

                        // Lets play around with two keypoints and try to draw from one, to the other
                        Keypoint leftShoulder = example_keypoints[5].scaled(Screen);
                        Keypoint leftWrist = example_keypoints[9].scaled(Screen); // left wrist keypoint
                        Keypoint rightShoulder = example_keypoints[6].scaled(Screen);

                        PointF leftShoulderPosition = leftShoulder.getPosition();
                        PointF rightShoulderPosition = rightShoulder.getPosition();

                        float posShoulder_x = leftShoulderPosition.x; //  x position in PIXELS
                        float posShoulder_y = leftShoulderPosition.y; //  y position in PIXELS

                        float posRightShoulder_x = rightShoulderPosition.x; //  x position in PIXELS
                        float posRightShoulder_y = rightShoulderPosition.y; //  y position in PIXELS

                        // Package the desired positions referenced from shoulder.
                        PointF desiredWristPosition = new PointF(posShoulder_x, posShoulder_y + (float) 2.5 * offset);
                        PointF desiredWristPosition2 = new PointF(posShoulder_x, posShoulder_y - (float) 2.5 * offset);

                        //float leftShoudlerDistance = leftElbow.calculateSquaredDistanceFromCoordinates(desiredElbowPostion);
                        float leftWristDistance = leftWrist.calculateSquaredDistanceFromCoordinates(desiredWristPosition);
                        float endLeftWristDistance = leftWrist.calculateSquaredDistanceFromCoordinates(desiredWristPosition2);

                        //String distance = String.valueOf(distanceFromDesired);
                        //Log.d("THIS IS THE DISTANCE", distance);
                        System.out.println(leftWristDistance);

                        if ((leftWristDistance <= 20000) && (endFlag2 == 0)) {
                            drawFlag = 1;
                            endFlag = 1;

                            if (drawFlag == 1) {
                                canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x, posShoulder_y + (float) 2.5 * offset, DrawUtils2.DEFAULT_PAINT); // DEFAULT_PAINT
                                canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x, posRightShoulder_y + (float) 2.5 * offset, DrawUtils2.DEFAULT_PAINT); // DEFAULT_PAINT


                                // OTHER LEFT draw the other pose in gray
                                canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x, posShoulder_y - (float) 2.5 * offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                                canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posShoulder_x, posShoulder_y - (float) 2.5 * offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT

                                drawFlag2 = 1; // enabled coloring other pose green
                                endFlag2 = 1;
                            }


                        }

                        if ((endFlag == 1)) {
                            //drawFlag2 = 1;

                            if (drawFlag2 == 1 && (endLeftWristDistance <= 20000)) {
                                canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x, posShoulder_y - (float) 2.5 * offset, DrawUtils2.DEFAULT_PAINT);
                                canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x, posShoulder_y - (float) 2.5 * offset, DrawUtils2.DEFAULT_PAINT);

                                endFlag2 = 0;
                                drawFlag2 = 0;
                                drawFlag = 0; // initially draw the gray
                                endFlag = 0;
                                reps += 1;
                            } else {
                                canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x, posShoulder_y - (float) 2.5 * offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                                canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x, posRightShoulder_y - (float) 2.5 * offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT

                            }
                        }


                        if (endFlag == 1 && drawFlag2 == 1) {
                            canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x, posShoulder_y - (float) 2.5 * offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                            canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x, posRightShoulder_y - (float) 2.5 * offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                        }

                        if (drawFlag == 0) {
                            // Draw from left shoulder guide for user in green. This is where they start
                            canvas.drawLine(posShoulder_x, posShoulder_y, posShoulder_x, posShoulder_y + (float) 2.5 * offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                            canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x, posRightShoulder_y + (float) 2.5 * offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT


                        }
                    }
                } else {
                    canvas.drawText("End of all exercises - Exit ", 50, 200, paint1);
                }
            }
            isComputing.set(false);
        });
    }

/*
    public void legSideRaise() {
        offset = 200;
        reps = 0;
        drawFlag = 0; // initially draw the gray
        drawFlag2 = 0;
        endFlag = 0;
        endFlag2 = 0;

        Paint paint = new Paint();
        Paint paint1 = new Paint();
        //paint1.setColor(android.R.color.green);
        paint1.setTextSize(75);

        setCallback(canvas -> {
            if (poseResult != null) {


                if (reps < 3) {
                    for (Pose pose : poseResult.getPoses()) {
                        // draw the skeleton, do not delete lol
                        pose.draw(canvas);


                        paint.setColor(Color.BLACK);
                        paint.setTextSize(100);
                        canvas.drawText("Reps : " + String.valueOf(reps), 50, 100, paint);
                        canvas.drawText("Leg Side Raise", 50, 200, paint);

                        // Get height and width of screen
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                        int height = displayMetrics.heightPixels; // returns a float representing a pixel value
                        int width = displayMetrics.widthPixels;  //
                        Size Screen = new Size(width, height);

                        // Retrieve the array of keypoints
                        Keypoint[] example_keypoints = pose.getKeypoints();

                        // Lets play around with two keypoints and try to draw from one, to the other
                        Keypoint leftHip = example_keypoints[11].scaled(Screen);
                        Keypoint rightKnee = example_keypoints[14].scaled(Screen);
                        Keypoint leftKnee = example_keypoints[13].scaled(Screen);
                        Keypoint leftAnkle = example_keypoints[15].scaled(Screen);

                        // for first set
                        PointF leftHipPosition = leftHip.getPosition();
                        PointF rightKneePosition = rightKnee.getPosition();
                        PointF leftKneePosition = leftKnee.getPosition();
                        PointF leftAnklePostiion = leftAnkle.getPosition();

                        float posLeftHip_x = leftHipPosition.x; //  x position in PIXELS
                        float posLeftHip_y = leftHipPosition.y; //  y position in PIXELS

                        float posLeftKnee_x = leftKneePosition.x; //  x position in PIXELS
                        float posLeftKnee_y = leftKneePosition.y; //  y position in PIXELS

                        float posRightKnee_x = rightKneePosition.x; //  x position in PIXELS
                        float posRightKnee_y = rightKneePosition.y; //  y position in PIXELS

                        float posleftAnkle_x = leftAnklePostiion.x; //  x position in PIXELS
                        float posleftAnkle_y = leftAnklePostiion.y; //  y position in PIXELS

                        // Package the desired positions referenced from shoulder.
                        // PointF desiredAnklePosition = new PointF(posRightKnee_x, posRightKnee_y);

                        PointF desiredKneePosition = new PointF(posLeftKnee_x, posLeftKnee_y+ offset);
                        PointF desiredKneePositionEnd = new PointF(posRightKnee_x + offset, posRightKnee_y-(float) 0.5*offset);

                        //float leftAnkleDistance = leftAnkle.calculateSquaredDistanceFromCoordinates(desiredAnklePosition);
                        float KneeDistance = leftKnee.calculateSquaredDistanceFromCoordinates(desiredKneePosition);
                        float endKneeDistance = leftKnee.calculateSquaredDistanceFromCoordinates(desiredKneePositionEnd);

                        //String distance = String.valueOf(distanceFromDesired);
                        //Log.d("THIS IS THE DISTANCE", distance);
                        System.out.println(leftAnkleDistance);

                        if ((leftAnkleDistance <= 20000) && (endFlag2 == 0)) {
                            drawFlag = 1;
                            endFlag = 1;

                            if (drawFlag == 1) {
                                //canvas.drawLine(posLeftHip_x, posLeftHip_y, posRightKnee_x, posRightKnee_y, DrawUtils2.DEFAULT_PAINT); // DEFAULT_PAINT
                                canvas.drawLine(posLeftHip_x, posLeftHip_y, posLeftHip_x, posLeftHip_y+offset, DrawUtils2.DEFAULT_PAINT); // DEFAULT_PAINT


                                // OTHER LEFT draw the other pose in gray
                                canvas.drawLine(posLeftHip_x, posLeftHip_y, posRightKnee_x + offset, posRightKnee_y-(float) 0.5*offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT

                                drawFlag2 = 1; // enabled coloring other pose green
                                endFlag2 = 1;
                            }


                        }

                        if ((endFlag == 1)) {
                            //drawFlag2 = 1;

                            if (drawFlag2 == 1 && (endKneeDistance <= 20000)) {
                                canvas.drawLine(posLeftHip_x, posLeftHip_y, posRightKnee_x + offset, posRightKnee_y-(float) 0.5*offset, DrawUtils2.DEFAULT_PAINT); // DEFAULT_PAINT

                                endFlag2 = 0;
                                drawFlag2 = 0;
                                drawFlag = 0; // initially draw the gray
                                endFlag = 0;
                                reps += 1;
                            } else {
                                canvas.drawLine(posLeftHip_x, posLeftHip_y, posRightKnee_x + offset, posRightKnee_y-(float) 0.5*offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT

                            }
                        }


                        if (endFlag == 1 && drawFlag2 == 1) {
                            canvas.drawLine(posLeftHip_x, posLeftHip_y, posRightKnee_x + offset, posRightKnee_y-(float) 0.5*offset, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                        }

                        if (drawFlag == 0) {
                            // Draw from left shoulder guide for user in green. This is where they start
                            //canvas.drawLine(posLeftHip_x, posLeftHip_y, posRightKnee_x, posRightKnee_y, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                            canvas.drawLine(posLeftHip_x, posLeftHip_y, posLeftHip_x, posLeftHip_y+offset, DrawUtils2.GRAY_PAINT);

                        }
                    }
                } else {
                    //canvas.drawText("Flap Arms", 50, 200, paint1);
                    //flapArms();
                }

            }
            isComputing.set(false);
        });
    }
}
*/

    public void neckRoll() {
        offset = 200;
        reps = 0;
        drawFlag = 0; // initially draw the gray
        drawFlag2 = 0;
        endFlag = 0;
        endFlag2 = 0;

        setCallback(canvas -> {
            if (poseResult != null) {
                Paint paint = new Paint();
                Paint paint1 = new Paint();
                paint1.setTextSize(75);
                if (reps < 3) {
                    for (Pose pose : poseResult.getPoses()) {
                        // draw the skeleton, do not delete lol
                        pose.draw(canvas);

                        paint.setColor(Color.BLACK);
                        paint.setTextSize(100);
                        canvas.drawText("Reps. : " + String.valueOf(reps), 50, 100, paint);
                        canvas.drawText("Flap Arms", 50, 200, paint1);

                        // Get height and width of screen
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                        int height = displayMetrics.heightPixels; // returns a float representing a pixel value
                        int width = displayMetrics.widthPixels;  //
                        Size Screen = new Size(width, height);


                        // Retrieve the array of keypoints
                        Keypoint[] example_keypoints = pose.getKeypoints();

                        // Lets play around with two keypoints and try to draw from one, to the other
                        Keypoint leftShoulder = example_keypoints[5].scaled(Screen);
                        Keypoint rightShoulder = example_keypoints[6].scaled(Screen);
                        Keypoint leftEar = example_keypoints[3].scaled(Screen); // left wrist keypoint
                        Keypoint rightEar = example_keypoints[4].scaled(Screen); // left wrist keypoint


                        PointF leftShoulderPosition = leftShoulder.getPosition();
                        PointF rightShoulderPosition = rightShoulder.getPosition();
                        
                        PointF leftEarPosition = leftEar.getPosition();
                        PointF rightEarPosition = rightEar.getPosition();
                        

                        float posLeftShoulder_x = leftShoulderPosition.x; //  x position in PIXELS
                        float posLeftShoulder_y = leftShoulderPosition.y; //  y position in PIXELS

                        float posRightShoulder_x = rightShoulderPosition.x; //  x position in PIXELS
                        float posRightShoulder_y = rightShoulderPosition.y; //  y position in PIXELS

                        float posLeftEar_x = leftEarPosition.x;
                        float posLeftEar_y = leftEarPosition.y;

                        float posRightEar_x = rightEarPosition.x;
                        float posRightEar_y = rightEarPosition.y;
                        
                        float leftEarDistance = leftEar.calculateSquaredDistanceFromCoordinates(leftShoulderPosition);
                        float rightEarDistance = rightEar.calculateSquaredDistanceFromCoordinates(rightShoulderPosition);

                        //String distance = String.valueOf(distanceFromDesired);
                        //Log.d("THIS IS THE DISTANCE", distance);
                        //system.out.println(leftEarDistance);
                        
                        if ((leftEarDistance <= 30000) && (endFlag2 == 0)) {
                            drawFlag = 1;
                            endFlag = 1;

                            if (drawFlag == 1) {
                                canvas.drawLine(posLeftShoulder_x, posLeftShoulder_y, posLeftShoulder_x + offset, posLeftShoulder_y, DrawUtils2.DEFAULT_PAINT); // DEFAULT_PAINT

                                // OTHER LEFT draw the other pose in gray
                                canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x - offset, posRightShoulder_y, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                                drawFlag2 = 1; // enabled coloring other pose green
                                endFlag2 = 1;
                            }


                        }

                        if ((endFlag == 1)) {
                            //drawFlag2 = 1;

                            if (drawFlag2 == 1 && (rightEarDistance <= 30000)) {
                                canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x - offset, posRightShoulder_y,  DrawUtils2.DEFAULT_PAINT);

                                endFlag2 = 0;
                                drawFlag2 = 0;
                                drawFlag = 0; // initially draw the gray
                                endFlag = 0;
                                reps += 1;
                            } else {
                                canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x - offset, posRightShoulder_y, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT

                            }
                        }


                        if (endFlag == 1 && drawFlag2 == 1) {
                            canvas.drawLine(posRightShoulder_x, posRightShoulder_y, posRightShoulder_x - offset, posRightShoulder_y, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT
                             }

                        if (drawFlag == 0) {
                            // Draw from left shoulder guide for user in green. This is where they start
                            canvas.drawLine(posLeftShoulder_x, posLeftShoulder_y, posLeftShoulder_x + offset, posLeftShoulder_y, DrawUtils2.GRAY_PAINT); // DEFAULT_PAINT

                        }

                    }
                } else {
                    canvas.drawText("End of all exercises - Exit ", 50, 200, paint1);
                }
            }
            isComputing.set(false);
        });

    }

}
