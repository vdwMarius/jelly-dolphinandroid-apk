package com.jellydolphin.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.hardware.*;
import android.view.*;
import android.widget.*;

public final class MainActivity extends Activity implements SensorEventListener {
    private DolphinView ocean;
    private SharedPreferences prefs;
    private SensorManager sensors;
    private long lastShake;
    private final int ink=0xFF0D2434,mint=0xFF76E8D0;
    private final int[] colours={0xFF76E8D0,0xFF75C9FF,0xFFBBA2FF,0xFFFFA6C5,0xFFFFCA80,0xFFE7F4FF};
    private final String[] names={"Mint","Ocean","Lilac","Rose","Peach","Pearl"};
    private final Button[] chips=new Button[6];
    private int selected;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);prefs=getSharedPreferences("jelly",MODE_PRIVATE);selected=Math.max(0,Math.min(5,prefs.getInt("colour",0)));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(ink);
        root.setOnApplyWindowInsetsListener((v,i)->{root.setPadding(0,i.getSystemWindowInsetTop(),0,i.getSystemWindowInsetBottom());return i;});setContentView(root);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(18),dp(6),dp(18),dp(6));root.addView(header);
        LinearLayout heading=new LinearLayout(this);heading.setOrientation(LinearLayout.VERTICAL);heading.addView(text("Jelly Dolphin",24,Color.WHITE));heading.addView(text("THE JELLY ROOM  /  3D PLAY",10,mint));header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        Button help=button("How to play",0xFF244857,Color.WHITE);header.addView(help);help.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setTitle("Your 3D jelly room").setMessage("Drag the dolphin to lift, gently squish and throw it. Tap its body to send a ripple through the mesh.\n\nPinch while holding an object to move it toward or away from the camera. The Depth slider also moves the dolphin through the room.\n\nDrag empty space to orbit. Pinch empty space to zoom.\n\nGrab or tap the peach ball. It collides with the dolphin. The floor, walls and blocks are solid.\n\nChange colour, squishiness and jelly clarity below. Turn gravity off for floating play. Reset recentres the room and camera.").setPositiveButton("Let's play",null).show());
        ocean=new DolphinView(this);ocean.room.colour=colours[selected];ocean.room.softness=prefs.getFloat("softness",.65f);ocean.room.clarity=prefs.getFloat("clarity",.65f);ocean.room.gravity=prefs.getBoolean("gravity",true);ocean.setContentDescription("3D dolphin room. Drag dolphin or ball, orbit empty space, pinch for depth or zoom.");root.addView(ocean,new LinearLayout.LayoutParams(-1,0,1));
        TextView hint=text("DRAG TO GRAB  ·  EMPTY SPACE TO ORBIT  ·  PINCH FOR DEPTH",9,0xFFACC8D0);hint.setGravity(Gravity.CENTER);root.addView(hint);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(16),0,dp(16),dp(5));root.addView(panel);
        LinearLayout palette=new LinearLayout(this);panel.addView(palette);
        for(int i=0;i<6;i++){final int index=i;Button b=new Button(this);chips[i]=b;b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(0,0,0,0);b.setTextColor(ink);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(48),1);cp.setMargins(dp(3),dp(4),dp(3),dp(4));palette.addView(b,cp);b.setOnClickListener(v->{selected=index;ocean.room.colour=colours[index];updateChips();save();});}updateChips();
        addSlider(panel,"Squishiness",Math.round(ocean.room.softness*100),"Squishiness, firm to extra jelly",(p)->{ocean.room.softness=p/100f;save();});
        addSlider(panel,"Jelly clarity",Math.round(ocean.room.clarity*100),"Jelly clarity, cloudy to clear",(p)->{ocean.room.clarity=p/100f;save();});
        addSlider(panel,"Depth · far / near",50,"Dolphin depth, far to near",(p)->{float z=(p-50)/50f*2.4f;ocean.queueEvent(()->ocean.room.body.setDepth(z));});
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);panel.addView(actions);
        Button shake=button("Shake",mint,ink);actions.addView(shake,new LinearLayout.LayoutParams(0,dp(48),1));shake.setOnClickListener(v->{ocean.queueEvent(()->ocean.room.body.impulse(2,4,-1.5f));ocean.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);});
        Button reset=button("Reset",0xFF274B5D,Color.WHITE);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,dp(48),1);rp.setMargins(dp(8),0,dp(8),0);actions.addView(reset,rp);reset.setOnClickListener(v->ocean.queueEvent(ocean.room::reset));
        Switch gravity=new Switch(this);gravity.setText("Gravity ");gravity.setTextColor(Color.WHITE);gravity.setTextSize(12);gravity.setContentDescription("Gravity");gravity.setChecked(ocean.room.gravity);actions.addView(gravity);gravity.setOnCheckedChangeListener((v,on)->{ocean.room.gravity=on;save();});
        sensors=(SensorManager)getSystemService(SENSOR_SERVICE);
    }
    private interface SliderChange {void change(int value);}
    private void addSlider(LinearLayout panel,String label,int initial,String description,SliderChange change){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView title=text(label,11,Color.WHITE);row.addView(title,new LinearLayout.LayoutParams(dp(110),-2));SeekBar s=new SeekBar(this);s.setMax(100);s.setProgress(initial);s.setContentDescription(description);s.setProgressTintList(ColorStateList.valueOf(mint));s.setThumbTintList(ColorStateList.valueOf(mint));row.addView(s,new LinearLayout.LayoutParams(0,dp(48),1));panel.addView(row);s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar v,int p,boolean user){if(user)change.change(p);}public void onStartTrackingTouch(SeekBar v){}public void onStopTrackingTouch(SeekBar v){}});}
    private void updateChips(){for(int i=0;i<6;i++){GradientDrawable d=new GradientDrawable();d.setColor(colours[i]);d.setCornerRadius(dp(12));chips[i].setBackground(d);chips[i].setText(i==selected?"✓":"");chips[i].setSelected(i==selected);chips[i].setContentDescription(names[i]+(i==selected?", selected":""));}}
    private void save(){prefs.edit().putInt("colour",selected).putFloat("softness",ocean.room.softness).putFloat("clarity",ocean.room.clarity).putBoolean("gravity",ocean.room.gravity).apply();}
    private Button button(String label,int bg,int fg){Button b=new Button(this);b.setText(label);b.setTextSize(11);b.setTextColor(fg);b.setAllCaps(false);b.setBackgroundTintList(ColorStateList.valueOf(bg));return b;}
    private TextView text(String s,int size,int colour){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(colour);return t;}
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    @Override protected void onResume(){super.onResume();ocean.resume();Sensor s=sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);if(s!=null)sensors.registerListener(this,s,SensorManager.SENSOR_DELAY_GAME);}
    @Override protected void onPause(){super.onPause();ocean.pause();sensors.unregisterListener(this);save();}
    @Override public void onAccuracyChanged(Sensor s,int accuracy){}
    @Override public void onSensorChanged(SensorEvent e){float x=e.values[0],y=e.values[1],z=e.values[2];long now=android.os.SystemClock.uptimeMillis();if(x*x+y*y+z*z>250&&now-lastShake>650){lastShake=now;float fx=-x*.2f,fy=Math.abs(y)*.2f,fz=z*.15f;ocean.queueEvent(()->ocean.room.body.impulse(fx,fy,fz));}}
}
