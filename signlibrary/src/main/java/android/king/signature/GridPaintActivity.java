package android.king.signature;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.king.signature.config.PenConfig;
import android.king.signature.databinding.SignActivityGridPaintBinding;
import android.king.signature.util.SignBitmapUtil;
import android.king.signature.util.SignDisplayUtil;
import android.king.signature.util.SignSystemUtil;
import android.king.signature.util.StatusBarCompat;
import android.king.signature.view.GridDrawable;
import android.king.signature.view.GridPaintView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;


/***
 * 名称：GridWriteActivity<br>
 * 描述：米格输入界面
 * 最近修改时间：
 * @since 2017/11/14
 * @author king
 */
public class GridPaintActivity extends AppCompatActivity implements View.OnClickListener, Handler.Callback {

    private ProgressDialog mSaveProgressDlg;

    private static final int MSG_SAVE_SUCCESS = 1;
    private static final int MSG_SAVE_FAILED = 2;
    private static final int MSG_WRITE_OK = 100;
    private Handler mHandler;

    private String mSavePath;
    private Editable cacheEditable;

    private SignActivityGridPaintBinding viewBinding;
    private int bgColor;
    private boolean isCrop;
    private String format;
    private String allText;
    private int currentIndex;
    private int lineSize;
    private int fontSize;
//    public static final String[] PEN_COLORS = new String[]{"#101010", "#027de9", "#0cba02", "#f9d403", "#ec041f"}
//    public static final int[] PEN_SIZES = new int[]{5, 15, 20, 25, 30}

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        viewBinding = SignActivityGridPaintBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        bgColor = getIntent().getIntExtra("background", Color.TRANSPARENT);
        lineSize = getIntent().getIntExtra("lineLength", 15);
        fontSize = getIntent().getIntExtra("fontSize", 50);
        isCrop = getIntent().getBooleanExtra("crop", false);
        format = getIntent().getStringExtra("format");
        allText = getIntent().getStringExtra("allText");

        PenConfig.PAINT_COLOR = Color.parseColor("#101010");
        PenConfig.PAINT_SIZE_LEVEL = 2;

        initView();
        initData();

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        super.onCreate(savedInstanceState);
        SignSystemUtil.disableShowInput(getApplicationContext(), viewBinding.editView);
    }

    /**
     * 设置主题颜色
     *
     * @param color 主题颜色
     */
    protected void setThemeColor(int color) {
        viewBinding.actionbar.getRoot().setBackgroundColor(color);
        StatusBarCompat.compat(this, color);
    }

    private void initData() {
        setThemeColor(PenConfig.THEME_COLOR);
        mHandler = new Handler(this);
    }

    /**
     * 初始化视图
     */
    private void initView() {
        viewBinding.deleteView.getRoot().setVisibility(View.INVISIBLE);
        viewBinding.deleteView.getRoot().setOnClickListener(this);
        viewBinding.actionbar.backBtn.setOnClickListener(this);
        viewBinding.actionbar.navigationTitle.setText("文字输入");

        refreshAllTextView();

        int size = getResources().getDimensionPixelSize(R.dimen.sign_grid_size);
        GridDrawable gridDrawable = new GridDrawable(size, size, Color.WHITE);
        viewBinding.paintView.setBackground(gridDrawable);

        viewBinding.paintView.setGetTimeListener(new GridPaintView.WriteListener() {
            @Override
            public void onWriteStart() {
                mHandler.removeMessages(MSG_WRITE_OK);
            }

            @Override
            public void onWriteCompleted(long time) {
                mHandler.sendEmptyMessageDelayed(MSG_WRITE_OK, 1000);
            }
        });

        int maxWidth = lineSize * SignDisplayUtil.dip2px(this, fontSize);
        if (!isCrop) {
            viewBinding.editView.setWidth(maxWidth + 2);
            viewBinding.editView.setMaxWidth(maxWidth);
        } else {
            viewBinding.editView.setWidth(maxWidth * 2 / 3);
            viewBinding.editView.setMaxWidth(maxWidth * 2 / 3);
        }
        viewBinding.editView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        viewBinding.editView.setLineHeight(SignDisplayUtil.dip2px(this, fontSize));
        viewBinding.editView.setHorizontallyScrolling(false);
        viewBinding.editView.requestFocus();
        if (bgColor != Color.TRANSPARENT) {
            viewBinding.svContainer.setBackgroundColor(bgColor);
        }
    }

    private void refreshAllTextView() {
        if (currentIndex < this.allText.length()) {
            SpannableString spannableString = new SpannableString(this.allText);
            //设置颜色
            spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.sign_pen_red)), currentIndex, currentIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            viewBinding.allTextView.setText(spannableString);
        }
    }

    private void changeDeleteShow() {
        int index = viewBinding.editView.getSelectionStart();
        if (index > 0) {
            if (this.viewBinding.deleteView.getRoot().getVisibility() == View.INVISIBLE) {
                AlphaAnimation mAnimation = new AlphaAnimation(0.0f, 1f);
                mAnimation.setDuration(250);
                mAnimation.setRepeatCount(0);
                mAnimation.setFillAfter(true);
                mAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        //empty
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        GridPaintActivity.this.viewBinding.deleteView.getRoot().setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                        //empty
                    }
                });
                this.viewBinding.deleteView.getRoot().setAnimation(mAnimation);

            }
        } else {
            if (this.viewBinding.deleteView.getRoot().getVisibility() == View.VISIBLE) {
                AlphaAnimation mAnimation = new AlphaAnimation(1f, 0.0f);
                mAnimation.setDuration(250);
                mAnimation.setRepeatCount(0);
                mAnimation.setFillAfter(true);
                mAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        //empty
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        GridPaintActivity.this.viewBinding.deleteView.getRoot().setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                        //empty
                    }
                });
                this.viewBinding.deleteView.getRoot().setAnimation(mAnimation);

            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHandler.removeMessages(MSG_WRITE_OK);
    }

    @Override
    protected void onDestroy() {
        viewBinding.paintView.release();
        super.onDestroy();
        mHandler.removeMessages(MSG_SAVE_FAILED);
        mHandler.removeMessages(MSG_SAVE_SUCCESS);
    }


    private void initSaveProgressDlg() {
        mSaveProgressDlg = new ProgressDialog(this);
        mSaveProgressDlg.setMessage("正在保存,请稍候...");
        mSaveProgressDlg.setCancelable(false);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_SAVE_FAILED:
                if (mSaveProgressDlg != null) {
                    mSaveProgressDlg.dismiss();
                }
                Toast.makeText(getApplicationContext(), "保存失败", Toast.LENGTH_SHORT).show();
                break;
            case MSG_SAVE_SUCCESS:
                if (mSaveProgressDlg != null) {
                    mSaveProgressDlg.dismiss();
                }
                Intent intent = new Intent();
                intent.putExtra(PenConfig.SAVE_PATH, mSavePath);
                setResult(RESULT_OK, intent);
                finish();
                break;
            case MSG_WRITE_OK:
                if (!viewBinding.paintView.isEmpty()) {
                    Bitmap bitmap = viewBinding.paintView.buildBitmap(isCrop, SignDisplayUtil.dip2px(GridPaintActivity.this, fontSize));
                    this.cacheEditable = viewBinding.editView.addBitmapToText(bitmap);
                    viewBinding.paintView.reset();
                    this.currentIndex = viewBinding.editView.getSelectionStart();
                    refreshAllTextView();
                    changeDeleteShow();
                    writeComplete();
                }
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v == viewBinding.deleteView.getRoot()) {
            this.cacheEditable = viewBinding.editView.deleteBitmapFromText();
            this.currentIndex = viewBinding.editView.getSelectionStart();
            if (this.currentIndex >= 1) {
                AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1.0f);
                alphaAnimation.setDuration(800);
                alphaAnimation.setInterpolator(new LinearInterpolator());
                alphaAnimation.setRepeatCount(0);
                alphaAnimation.setRepeatMode(Animation.REVERSE);
                GridPaintActivity.this.viewBinding.deleteView.getRoot().startAnimation(alphaAnimation);
            }
            refreshAllTextView();
            changeDeleteShow();
        } else if (v == viewBinding.actionbar.backBtn) {
            if (viewBinding.editView.getText() != null && viewBinding.editView.getText().length() > 0) {
                showQuitTip();
                changeDeleteShow();
            } else {
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    private void writeComplete() {
        if (currentIndex == this.allText.length()) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("提示")
                    .setMessage("是否保存手写内容？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", (dialog, which) -> save());
            builder.show();
        }
    }

    /**
     * 保存
     */
    private void save() {
        if (null != viewBinding.editView.getText() && viewBinding.editView.getText().length() == 0) {
            Toast.makeText(getApplicationContext(), "没有写入任何文字", Toast.LENGTH_SHORT).show();
            return;
        }
        //先检查是否有存储权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "没有读写存储的权限", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mSaveProgressDlg == null) {
            initSaveProgressDlg();
        }
        viewBinding.editView.clearFocus();
        viewBinding.editView.setCursorVisible(false);

        mSaveProgressDlg.show();
        new Thread(() -> {
            if (PenConfig.FORMAT_JPG.equals(format) && bgColor == Color.TRANSPARENT) {
                bgColor = Color.WHITE;
            }
            Bitmap bm = getWriteBitmap(bgColor);
            bm = SignBitmapUtil.clearBlank(bm, 20, bgColor);

            if (bm == null) {
                mHandler.obtainMessage(MSG_SAVE_FAILED).sendToTarget();
                return;
            }
            mSavePath = SignBitmapUtil.saveImage(GridPaintActivity.this, bm, 100, format);
            if (mSavePath != null) {
                mHandler.obtainMessage(MSG_SAVE_SUCCESS).sendToTarget();
            } else {
                mHandler.obtainMessage(MSG_SAVE_FAILED).sendToTarget();
            }
        }).start();
    }

    /**
     * 获取EdiText截图
     *
     * @param bgColor 背景颜色
     * @return EdiText截图
     */
    private Bitmap getWriteBitmap(int bgColor) {
        int w = 0;
        int h = 0;
        for (int i = 0; i < viewBinding.svContainer.getChildCount(); i++) {
            h += viewBinding.svContainer.getChildAt(i).getHeight();
            w += viewBinding.svContainer.getChildAt(i).getWidth();
        }
        Bitmap bitmap;
        try {
            bitmap = Bitmap.createBitmap(w, h,
                    Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            bitmap = Bitmap.createBitmap(w, h,
                    Bitmap.Config.ARGB_4444);
        }
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(bgColor);
        viewBinding.svContainer.draw(canvas);
        return bitmap;
    }

    @Override
    public void onBackPressed() {
        if (viewBinding.editView.getText() != null && viewBinding.editView.getText().length() > 0) {
            showQuitTip();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    /**
     * 弹出退出提示
     */
    private void showQuitTip() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示")
                .setMessage("当前文字未保存，是否退出？")
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (dialog, which) -> {
                    setResult(RESULT_CANCELED);
                    finish();
                });
        builder.show();
    }

}
