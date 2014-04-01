package com.ygl.guaguale.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.widget.TextView;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 刮刮乐涂层实现类
 * Created by yanggavin on 14-3-31.
 */
public class GuaGuaViewClass extends TextView {

    private Context _context = null;                  // 上下文
    private int _prizeNum;                            // 中奖金额
    private Canvas _topCanvas = null;                 // 涂层画板
    private Bitmap _topBitmap = null;                 // 涂层位图
    private Path _path = new Path();                  // 画笔路径
    private Paint _paint = null;                      // 手指画笔
    private int _x = 0;                               // 当前触点X坐标
    private int _y = 0;                               // 当前触点Y坐标
    private boolean _showPrizeMsg = false;            // 是否显示中奖信息
    private int _showMsgTime = 0;                     // 为避免重复提示，需要对提示进行计数
    private boolean _readyToPlay = false;             // 是否可以开始玩


    private Handler _handler = new Handler(){   // 处理定时调用的事件句柄
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            if (msg.what == 1)
                PlayAgain(HongBaoClass.GetRandomValue());
        }
    };

    /**
     * 构造函数
     * @param context   上下文
     * @param prizeNum  中奖金额
     */
    public GuaGuaViewClass(Context context, int prizeNum) {
        super(context);

        // 内部对象赋值
        _prizeNum = prizeNum;
        _context = context;

        // 试图初始化
        InitView();
    }

    /**
     * 视图内部初始化并展示
     */
    private void InitView(){

        // 设置视图背景图片
        SetBackgroundImageForView();
        // 设置表层涂层
        SetCoating();
        // 初始化手指画笔
        _paint = GetFingerPaint();
        // 设置变量
        _readyToPlay = true;
    }

    /**
     * 再刮一次
     * @param prizeNum  中奖金额
     */
    public void PlayAgain(int prizeNum){
        _prizeNum = prizeNum;
        _showPrizeMsg = false;
        _showMsgTime = 0;

        // 设置视图背景图片
        SetBackgroundImageForView();
        // 设置表层涂层
        SetCoating();

        // 设置变量
        _readyToPlay = true;
    }

    /**
     * 清除剩余灰色涂层
     */
    private void ClearCoating(){
        // 将涂层位图颜色设置为透明
        _topBitmap.eraseColor(0);
        // 重绘
        postInvalidate();
    }

    /**
     * 试图绘制方法
     * @param canvas    试图默认canvas
     */
    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);

        // 将path绘制到topBitmap涂层上
        _topCanvas.drawPath(_path, _paint);
        // 将重绘后的topBitmap涂层绘制到View得画板中显示
        canvas.drawBitmap(_topBitmap, 0, 0, null);
    }

    /**
     * 滑动触摸事件
     * @param event 事件句柄
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // 如果没有初始化完成，不响应触摸事件
        if (!_readyToPlay)
            return true;

        int currX = (int) event.getX();
        int currY = (int) event.getY();

        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:{
                _path.reset();
                _x = currX;
                _y = currY;
                _path.moveTo(_x, _y);
                break;
            }
            case MotionEvent.ACTION_MOVE:{
                // 二次贝塞尔，实现平滑曲线；previousX, previousY为操作点，cX, cY为终点
                _path.quadTo(_x, _y, currX, currY);
                _x = currX;
                _y = currY;
                // 重绘View，再非UI线程中调用
                postInvalidate();
                // 检查关键点是否已被刮开
                CheckKeyPoint();
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:{
                _path.reset();
                // 显示刮奖结果
                ShowPrizeMsgToUser();
                break;
            }
        }

        return true;    // 事件执行完毕
    }

    /**
     * 设置涂层
     */
    private void SetCoating(){
        // 等面积位图
        _topBitmap = Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888);
        // 将涂层位图载入到涂层画板
        _topCanvas = new Canvas(_topBitmap);
        // 设置涂层颜色
        _topCanvas.drawColor(Color.GRAY);
        // 添加提示文字到涂层
        Paint paint = new Paint();
        paint.setTextSize(120);
        paint.setColor(Color.DKGRAY);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setAntiAlias(true);
        _topCanvas.drawText(getResources().getString(R.string.string_words_on_coating), 150, 330, paint);
    }

    /**
     * 设置试图的背景图片
     */
    private void SetBackgroundImageForView(){

        // 设置视图背景图片
        Bitmap bgBitmap = Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bgBitmap);
        int bgColor = Color.parseColor("#DDDDDD");
        canvas.drawColor(bgColor);

        // 将中奖图标绘制到画板中
        int imgResId = GetBackgroundImageResIdByPrize();
        Drawable drawable = getResources().getDrawable(imgResId);
        drawable.setBounds(255, 105, 645, 495);
        drawable.draw(canvas);

        // 实例化可绘制位图，并绘制为视图的背景图
        BitmapDrawable bd = new BitmapDrawable(getResources(), bgBitmap);
        setBackground(bd);
    }

    /**
     * 给用户显示刮奖结果信息
     */
    private void ShowPrizeMsgToUser(){
        String msgTitle = "";
        String msg = "";

        if (!_showPrizeMsg || _showMsgTime > 0)
           return;

        // 刮奖结果提示计数
        _showMsgTime++;
        _readyToPlay = false;

        // 提示信息内容
        if (_prizeNum == 0) {
            Random r = new Random(System.currentTimeMillis());
            int index = r.nextInt(SysConst.ARRAY_LOSE_MESSAGE.length);
            msg = SysConst.ARRAY_LOSE_MESSAGE[index];
            msgTitle = getResources().getString(R.string.string_lose);
        }
        else{
            msg = String.format(getResources().getString(R.string.string_win_msg), _prizeNum);
            msgTitle = getResources().getString(R.string.string_win);
        }

        // 弹出提示框
        new AlertDialog.Builder(_context)
                .setTitle(msgTitle)
                .setMessage(msg)
                .setPositiveButton(getResources().getString(R.string.button_play_again),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // 获取下一张刮刮卡
                                GetAnotherGuaGuaKa();

                            }
                        })
                .setNegativeButton(getResources().getString(R.string.button_quit),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                System.exit(0);
                            }
                        })
                .setCancelable(false)
                .show();
    }

    /**
     * 获取下一张刮刮卡
     */
    private void GetAnotherGuaGuaKa(){

        // 设置涂层颜色
        _topCanvas.drawColor(Color.GRAY);
        // 添加提示文字到涂层
        Paint paint = new Paint();
        paint.setTextSize(50);
        paint.setColor(Color.DKGRAY);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setAntiAlias(true);
        _topCanvas.drawText(
                getResources().getString(R.string.string_get_another_card),
                200,
                310,
                paint);

        // 重绘
        postInvalidate();

        Timer timer = new Timer();
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                _handler.sendEmptyMessage(1);
            }
        };
        long delay = (new Random(System.currentTimeMillis())).nextInt(1000);
        timer.schedule(tt, delay);
    }

    /**
     * 检查关键像素点是否已被涂开，如果各关键点已被涂开，则直接显示结果
     */
    private void CheckKeyPoint(){

        // 如果涂层已被全部刮开，则不进行再次判断
        if (_showPrizeMsg)
            return;

        int[] xArr = new int[] { 255, 255, 645, 645, 450, 305, 305, 595, 595};
        int[] yArr = new int[] { 105, 495, 105, 495, 300, 155, 445, 155, 495};

        int num = 0;
        for (int i=0; i<xArr.length; i++){
            if (_topBitmap.getPixel(xArr[i], yArr[i]) == 0)
                num++;
        }

        // 5个关键点，只要大于4个，即取消涂层，提示中奖信息
        if (num >= 6){
            ClearCoating();
            _showPrizeMsg = true;
        }
    }

    /**
     * 根据中奖金额，获取不同的背景图案资源Id
     * @return  图片资源Id
     */
    private int GetBackgroundImageResIdByPrize(){
        int id = 0;

        Random r = new Random(System.currentTimeMillis());
        String imgName = "";
        if (_prizeNum > 0) {
            imgName = SysConst.ARRAY_WIN_IMAGE_NAME[r.nextInt(SysConst.ARRAY_WIN_IMAGE_NAME.length)];
        }
        else {
            imgName = SysConst.ARRAY_LOSE_IMAGE_NAME[r.nextInt(SysConst.ARRAY_LOSE_IMAGE_NAME.length)];
        }

        // 根据文件名获取图片id
        id = getResources().getIdentifier(imgName, "drawable", _context.getPackageName());
        if (id == 0){
            if (_prizeNum > 0)
                id = R.drawable.win01;
            else
                id = R.drawable.lose01;
        }

        return id;
    }

    /**
     * 初始化手指画笔
     * @return  画笔
     */
    private Paint GetFingerPaint(){
        Paint paint = new Paint();

        // 初始化画笔
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);     // 消除锯齿
        paint.setAntiAlias(true);                  // 设置是否使用抗锯齿功能，会消耗较大资源，绘制图形速度会变慢
        paint.setDither(true);                     // 设定是否使用图像抖动处理，会使绘制出来的图片颜色更加平滑和饱满，图像更加清晰
        paint.setStyle(Paint.Style.STROKE);        // 画笔样式
        paint.setStrokeWidth(80);                  // 当画笔样式为STROKE或FILL_OR_STROKE时，设置笔刷的粗细度
        paint.setStrokeCap(Paint.Cap.ROUND);       // 当画笔样式为STROKE或FILL_OR_STROKE时，设置笔刷的图形样式
        paint.setStrokeJoin(Paint.Join.ROUND);     // 结合处的样子
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));     // 设置图形重叠时的处理方式
        paint.setAlpha(0);                         // 设置绘制图形的透明度

        return paint;
    }
}
