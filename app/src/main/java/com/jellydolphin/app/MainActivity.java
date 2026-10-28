package com.jellydolphin.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.hardware.*;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity implements SensorEventListener {
    private DolphinView ocean;
    private SharedPreferences prefs;
    private SensorManager sensors;
    private long lastShake;
    private final int ink=Color.rgb(13,36,52), mint=Color.rgb(118,232,208);
    private final int[] colours={0xFF76E8D0,0xFF75C9FF,0xFFBBA2FF,0xFFFFA6C5,0xFFFFCA80,0xFFE7F4FF};
    private final String[] names={"Mint","Ocean","Lilac","Rose","Peach","Pearl"};
    private final Button[] chips=new Button[6];
    private int selected;
    private TextView status;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("jelly",MODE_PRIVATE);
        selected=prefs.getInt("colour",0); if(selected<0||selected>=6)selected=0;
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(ink);
        root.setPadding(dp(22),dp(12),dp(22),dp(10));
        root.setOnApplyWindowInsetsListener((v,insets)-> { root.setPadding(dp(22),insets.getSystemWindowInsetTop()+dp(10),dp(22),insets.getSystemWindowInsetBottom()+dp(10)); return insets; });
        setContentView(root);
        TextView eyebrow=text("POCKET OCEAN  /  01",11,mint); eyebrow.setLetterSpacing(.22f); root.addView(eyebrow);
        TextView title=text("Jelly Dolphin",32,Color.WHITE); title.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL)); root.addView(title);
        root.addView(text("A little squish. A little calm.",14,0xFFA7BFCC));
        ocean=new DolphinView(this); ocean.setContentDescription("Interactive jelly dolphin. Drag to stretch and throw; tap to squish. Use Shake for an accessible bounce.");
        ocean.colour=colours[selected]; ocean.physics.softness=prefs.getFloat("softness",.65f);
        ocean.physics.gravity=prefs.getBoolean("gravity",false);
        root.addView(ocean,new LinearLayout.LayoutParams(-1,0,1));
        status=text("TAP TO SQUISH  ·  DRAG & RELEASE TO THROW",10,0xFF9CB8C6); status.setGravity(17); status.setLetterSpacing(.06f); root.addView(status);
        LinearLayout panel=new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(16),dp(14),dp(16),dp(10)); panel.setBackground(shape(0xFF183747,24));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2); pp.topMargin=dp(16); root.addView(panel,pp);
        TextView colourLabel=text("YOUR DOLPHIN   ·   "+names[selected],11,0xFFDAE9EF); panel.addView(colourLabel);
        LinearLayout row=new LinearLayout(this); row.setGravity(17); panel.addView(row);
        for(int i=0;i<6;i++) { final int index=i; Button b=new Button(this); chips[i]=b; b.setMinWidth(0); b.setMinimumWidth(0); b.setPadding(0,0,0,0); b.setTextColor(ink); b.setTextSize(19); b.setContentDescription(names[i]+" dolphin");
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(48),1); cp.setMargins(dp(3),dp(10),dp(3),dp(10)); row.addView(b,cp);
            b.setOnClickListener(v->{selected=index; ocean.colour=colours[index]; updateChips(); colourLabel.setText("YOUR DOLPHIN   ·   "+names[index]); save();}); }
        updateChips();
        TextView softness=text("Squishiness",14,Color.WHITE); panel.addView(softness);
        SeekBar slider=new SeekBar(this); slider.setMax(100); slider.setProgress(Math.round(ocean.physics.softness*100)); slider.setContentDescription("Squishiness, firm to extra jelly"); slider.setProgressTintList(ColorStateList.valueOf(mint)); slider.setThumbTintList(ColorStateList.valueOf(mint)); panel.addView(slider,new LinearLayout.LayoutParams(-1,dp(38)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ public void onProgressChanged(SeekBar s,int p,boolean user){ocean.physics.softness=p/100f;softness.setText("Squishiness  ·  "+p+"%");if(user)save();} public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){ocean.physics.poke(1);} });
        LinearLayout labels=new LinearLayout(this); TextView firm=text("Firm",10,0xFF9CB8C6), jelly=text("Extra jelly",10,0xFF9CB8C6); labels.addView(firm,new LinearLayout.LayoutParams(0,-2,1)); labels.addView(jelly); panel.addView(labels);
        LinearLayout actions=new LinearLayout(this); actions.setGravity(Gravity.CENTER_VERTICAL); panel.addView(actions);
        Button shake=button("Shake",mint,ink); actions.addView(shake,new LinearLayout.LayoutParams(0,dp(48),1)); shake.setOnClickListener(v->{ocean.physics.shake(360,-460);ocean.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);});
        Button reset=button("Reset",0xFF274B5D,Color.WHITE); LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,dp(48),1); rp.setMargins(dp(8),0,dp(8),0); actions.addView(reset,rp); reset.setOnClickListener(v->ocean.physics.reset());
        Switch gravity=new Switch(this); gravity.setText("Gravity "); gravity.setTextSize(12); gravity.setTextColor(Color.WHITE); gravity.setChecked(ocean.physics.gravity); gravity.setContentDescription("Gravity"); actions.addView(gravity); gravity.setOnCheckedChangeListener((v,on)->{ocean.physics.gravity=on;save();});
        sensors=(SensorManager)getSystemService(SENSOR_SERVICE);
    }
    private void updateChips(){ for(int i=0;i<6;i++){chips[i].setBackground(shape(colours[i],14));chips[i].setText(i==selected?"✓":"");chips[i].setSelected(i==selected);chips[i].setContentDescription(names[i]+(i==selected?", selected":""));} }
    private void save(){ prefs.edit().putInt("colour",selected).putFloat("softness",ocean.physics.softness).putBoolean("gravity",ocean.physics.gravity).apply(); }
    private Button button(String label,int bg,int fg){Button b=new Button(this);b.setText(label);b.setTextColor(fg);b.setTextSize(12);b.setAllCaps(false);b.setBackgroundTintList(ColorStateList.valueOf(bg));return b;}
    private GradientDrawable shape(int colour,int radius){GradientDrawable d=new GradientDrawable();d.setColor(colour);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String s,int size,int colour){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(colour);t.setPadding(0,dp(3),0,dp(3));return t;}
    private int dp(float x){return Math.round(x*getResources().getDisplayMetrics().density);}
    @Override protected void onResume(){super.onResume();if(ocean!=null)ocean.resume();Sensor a=sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);if(a!=null)sensors.registerListener(this,a,SensorManager.SENSOR_DELAY_GAME);}
    @Override protected void onPause(){super.onPause();ocean.pause();sensors.unregisterListener(this);save();}
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
    @Override public void onSensorChanged(SensorEvent e){float x=e.values[0],y=e.values[1],z=e.values[2];long now=android.os.SystemClock.uptimeMillis();if(x*x+y*y+z*z>250 && now-lastShake>650){lastShake=now;ocean.physics.shake(-x*30,y*-25);}}
}
