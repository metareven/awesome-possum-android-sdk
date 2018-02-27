package com.telenor.possumauth.example.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;

import com.telenor.possumauth.example.R;
import com.telenor.possumcore.constants.DetectorType;

public class IconWheel extends View {
    private Paint offlinePaint = new Paint();
    private Paint onlinePaint = new Paint();
    private static int width;
    private static int height;
    private float centerX, centerY;
    private static float iconWidth;
    private static float iconHeight;
    private static float hypotenuse;

    private SparseArray<SensorContainer> sensors = new SparseArray<>();

/*    @Override
    public void changeInCombinedTrust(float combinedTrustScore, String status, String graphName) {
        AwesomePossum.sendDetectorStatus(getContext());
    }

    @Override
    public void changeInDetectorTrust(int detectorType, float newTrustScore, String status, String graphName) {
        SensorContainer sensor = sensors.get(detectorType);
        if (sensor != null) {
            sensor.setTraining(Float.parseFloat(status)<1);
            sensor.invalidateIcon();
        }
    }

    @Override
    public void failedToAscertainTrust(Exception exception) {

    }

    @Override
    public void possumMessageReceived(String msgType, String message) {
        if (Messaging.DETECTORS_STATUS.equals(msgType)) {
            JsonArray sensorsArray = (JsonArray) new JsonParser().parse(message);
            for (int i = 0; i < sensorsArray.size(); i++) {
                JsonObject sensor = sensorsArray.get(i).getAsJsonObject();
                int type = sensor.get("type").getAsInt();
                boolean enabled = sensor.get("isEnabled").getAsBoolean();
                boolean available = sensor.get("isAvailable").getAsBoolean();
                SensorContainer sensorContainer = sensors.get(type);
                if (sensorContainer != null) {
                    sensorContainer.setEnabled(enabled);
                    sensorContainer.setAvailable(available);
                    sensorContainer.invalidateIcon();
                }
            }
        }
    }

    @Override
    public void possumFaceFound(byte[] dataReceived) {

    }

    @Override
    public void possumImageSnapped(byte[] dataReceived) {

    }

    @Override
    public void possumFaceCoordsReceived(int[] xCoords, int[] yCoords) {

    }*/

    private enum SensorStatus {
        RED,
        GREEN
    }

    private class SensorContainer {
        private Bitmap bitmap;
        private double angleRad;
        private Rect rect;
        private boolean enabled;
        private boolean available;
        private boolean isTraining = true;

        SensorContainer(Bitmap bitmap, float angle) {
            this.bitmap = bitmap;
            angleRad = (Math.PI / 180) * angle;
        }

        Bitmap bitmap() {
            return bitmap;
        }

        Rect rect() {
            if (rect == null) {
                updateRect();
            }
            return rect;
        }

        void setEnabled(boolean enabled) {
            if (this.enabled != enabled) {
                this.enabled = enabled;
                invalidateIcon();
            }
        }

        void setTraining(boolean isTraining) {
            this.isTraining = isTraining;
            invalidateIcon();
        }

        void setAvailable(boolean available) {
            if (this.available != available) {
                this.available = available;
                invalidateIcon();
            }
        }

        SensorStatus sensorStatus() {
            return (!enabled || !available || isTraining) ? SensorStatus.RED : SensorStatus.GREEN;
        }

        void updateRect() {
            rect = new Rect((int) (centerX - iconWidth / 2), (int) (centerY - iconHeight / 2), (int) (centerX + iconWidth / 2), (int) (centerY + iconHeight / 2));
            rect.offset((int) (hypotenuse * Math.cos(angleRad)), (int) (hypotenuse * Math.sin(angleRad)));
        }

        void invalidateIcon() {
            IconWheel.this.invalidate(rect());
        }
    }

    public IconWheel(Context context) {
        super(context);
        init();
    }

    public IconWheel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public IconWheel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        sensors.put(DetectorType.Accelerometer, new SensorContainer(
                BitmapFactory.decodeResource(getResources(), R.drawable.ic_screen_rotation_black_48dp),
                320));
        sensors.put(DetectorType.Bluetooth, new SensorContainer(
                BitmapFactory.decodeResource(getResources(), R.drawable.ic_bluetooth_black_48dp),
                0
        ));
        sensors.put(DetectorType.Audio, new SensorContainer(
                BitmapFactory.decodeResource(getResources(), R.drawable.ic_volume_up_black_48dp),
                40
        ));
        sensors.put(DetectorType.Image, new SensorContainer(
                BitmapFactory.decodeResource(getResources(), R.drawable.ic_camera_front_black_48dp),
                140
        ));
        sensors.put(DetectorType.Position, new SensorContainer(
                BitmapFactory.decodeResource(getResources(), R.drawable.ic_location_on_black_48dp),
                180
        ));
        sensors.put(DetectorType.Network, new SensorContainer(
                BitmapFactory.decodeResource(getResources(), R.drawable.ic_wifi_black_48dp),
                220
        ));

        offlinePaint.setColorFilter(new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_IN));
        onlinePaint.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#009900"), PorterDuff.Mode.SRC_IN));

        iconWidth = pixelValue(30);
        iconHeight = pixelValue(30);
        hypotenuse = pixelValue(140);

//        AwesomePossum.addTrustListener(getContext(), this);
//        AwesomePossum.addMessageListener(getContext(), this);
//        AwesomePossum.sendDetectorStatus(getContext());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size;
        width = getMeasuredWidth();
        height = getMeasuredHeight();
        int widthWithoutPadding = width - getPaddingLeft() - getPaddingRight();
        int heightWithoutPadding = height - getPaddingTop() - getPaddingBottom();
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (heightMode != MeasureSpec.UNSPECIFIED && widthMode != MeasureSpec.UNSPECIFIED) {
            size = widthWithoutPadding > heightWithoutPadding ? heightWithoutPadding : widthWithoutPadding;
        } else {
            size = Math.max(heightWithoutPadding, widthWithoutPadding);
        }
        setMeasuredDimension(size + getPaddingLeft() + getPaddingRight(),
                size + getPaddingTop() + getPaddingBottom());
    }

    private float pixelValue(int dpValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, getResources().getDisplayMetrics());
    }

    @Override
    protected void onSizeChanged(int newWidth, int newHeight, int oldWidth, int oldHeight) {
        super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
        int minValue = Math.min(newWidth, newHeight);
        int xOffset = newWidth - minValue;
        int yOffset = newHeight - minValue;
        int paddingTop = this.getPaddingTop() + (yOffset / 2);
        int paddingBottom = this.getPaddingBottom() + (yOffset / 2);
        int paddingLeft = this.getPaddingLeft() + (xOffset / 2);
        int paddingRight = this.getPaddingRight() + (xOffset / 2);
        width = getWidth();
        height = getHeight();
        RectF area = new RectF(
                paddingLeft,
                paddingTop,
                width - paddingRight,
                height - paddingBottom);
        centerX = area.centerX();
        centerY = area.centerY();
        for (int i = 0; i < sensors.size(); i++) {
            sensors.get(sensors.keyAt(i)).updateRect();
        }
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < sensors.size(); i++) {
            Paint paint;
            SensorContainer sensor = sensors.get(sensors.keyAt(i));
            switch (sensor.sensorStatus()) {
                case GREEN:
                    paint = onlinePaint;
                    break;
                default:
                    paint = offlinePaint;
                    break;
            }
            canvas.drawBitmap(sensor.bitmap(), null, sensor.rect(), paint);
        }
    }
}