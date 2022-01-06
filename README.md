# Where

## 简介

Where是一个Android debug插件，在debug环境下，可以通过三指点击，快速识别当前Activity，Fragment，Dialog，DialogFragment的名称，帮助我们快速定位页面。通过ASM字节码插桩在实现在debug开发下无侵入增加功能。

## 简单使用

1. 确保项目使用androidx并且基类是继承于AppCompat包下的，否则无效

2. 在project级别的build.gradle加入私有仓库地址：

   ```groovy
   allprojects {
       repositories {
           ....
            maven { url 'https://jitpack.io' }
       }
   }
   ```

3. 在project级别的build.gradle引入插件依赖：

   ```groovy
       dependencies {
          ...
           classpath 'com.github.anymao.where:where:master-SNAPSHOT'
           // NOTE: Do not place your application dependencies here; they belong
           // in the individual module build.gradle.kts.kts files
       }
   ```

4. 在application module下的build.gradle引入插件

   ```groovy
   plugins {
       ...
       id("where")
   }
   ```

5. 完成！接下来 run app即可

## 使用效果

1. 在Activity界面触发：

   如果触发区域是Fragment，则会一直递归显示到点击的那个Fragment上面：<img src="http://cdn.1or1.icu/46081b25a98f8f97bb2cc68cd3ade96.jpg" style="zoom: 45%;" /><img src="http://cdn.1or1.icu/5ea50bfd5f7301017dcdb2d86ff4807.jpg" style="zoom: 45%;" />

2. 如果是三指触发Dialog，则会显示这个Dialog的全类名：<img src="http://cdn.1or1.icu/a4fc4832a31586f644619fd73283435.jpg" style="zoom: 67%;" />

3. 如果触发的是DialogFragment则会显示这个DialogFragment的类名：<img src="http://cdn.1or1.icu/8f40a4150665a815a1c8b7b4c0b345f.jpg" style="zoom:67%;" />

4. logcat会同时打印出来日志：![](http://cdn.1or1.icu/1623472744.png)

## 设计思路

1. 以Activity为例，触摸事件是最先到达Activity#dispatchTouchEvent的，所以如果我们在这个方法里面插入一段代码，在某些手势下会触发toast，toast的内容是当前Activity的类名称，这么看来似乎是可行的。

2. 检测Fragment，当事件到达Activity#dispatchTouchEvent后，我们先判断是不是触发的那个手势，例如本例中，触发手势为action in listOf(MotionEvent.ACTION_POINTER_UP) && event.pointerCount == 3（三指，并且有一个手指抬起来）会触发，在触发后，我们可以通过此event的触摸点坐标，再结合FragmentManager来遍历所有的Fragment，来找到我们的触摸点在哪些Fragment的区域内，这里我们通过getGlobalVisibleRect方法的作用是获取视图在屏幕坐标中的可视区域。

3. 如法炮制，我们可以hook AppCompatDialog#dispatchTouchEvent方法来达到此目的。

4. 特殊情况，对于DialogFragment，一般我们显示出来的视图还是AppCompatDialog但是一般情况下，我们的逻辑是触发时候显示的是DialogFragment的全类名，所以这里我们的想法是，在Dialog里面设置一个变量whereName，默认情况下此值是getClass().getCanonicalName(),这就能达到如果我们继承于AppCompatDialog的话，触发时候显示是Dialog的类名；而在DialogFragment中，在onCreateDialog之后会调用setupDialog，这是一个好时机我们来修改AppCompatDialog#whereName的值，由此，DialogFragment的显示逻辑也完成了。

5. 基于前4条，如果我们的项目中的Activity，Dialog，DialogFragment都是继承于我们自己的Base*的情况下（也就是说我们有一个统一的基类并且我们有源码）,那么按照这个思路去实现即可，但是需要注意的是release模式下关闭此功能的实现，但如果我们项目中的基类不是统一的，或者引入了大量的第三方库，对于没有插入hook代码的Activity或者Dialog就没办法使用这个功能了，所以我们需要借助于字节码插桩，在debug模式下将我们的代码插桩到AppCompatActivity,AppCompatDialog,AppCompatDialogFragment中，这样既不影响release模式，也能兼顾三方库中的Activity和Dialog等。

6. 选择hook点，最开始，我思考能不能一hook到底，直接代码插入到Activity里面去，我通过Transform扫描所有的jar包，并没有扫描到Activity这个类，仔细想想也能明白，这个是android.jar里面提供的类，不会打入我们的apk里面，那么我们就hook其常用的基类（AppCompatActivity,AppCompatDialog,AppCompatDialogFragment），如果想知道我们到底能hook哪些类，可以将生成的apk反编译，使用jd-gui查看里面打进去了哪些类，这些应该都是我们可以hook的点。

7. tips:gradle插件开发的时候，可以先选择buildSrc模式开发调试，避免使用插件模式还需要发布的步骤，插件稳定了再考虑发布出来，Android Studio4.2对buildSrc存在一下问题，在4.1上面可以编译的代码在4.2上面不行了，目前需要注意这点。

8. 看下编译生成的字节码文件：

   1. AppCompatActivity

      ```java
      package androidx.appcompat.app;
      
      import android.app.Activity;
      import android.content.Context;
      import android.content.Intent;
      import android.content.res.Configuration;
      import android.content.res.Resources;
      import android.os.Build;
      import android.os.Bundle;
      import android.util.DisplayMetrics;
      import android.view.KeyEvent;
      import android.view.Menu;
      import android.view.MenuInflater;
      import android.view.MenuItem;
      import android.view.MotionEvent;
      import android.view.View;
      import android.view.ViewGroup;
      import android.view.Window;
      import androidx.activity.contextaware.OnContextAvailableListener;
      import androidx.appcompat.view.ActionMode;
      import androidx.appcompat.widget.Toolbar;
      import androidx.appcompat.widget.VectorEnabledTintResources;
      import androidx.core.app.ActivityCompat;
      import androidx.core.app.NavUtils;
      import androidx.core.app.TaskStackBuilder;
      import androidx.fragment.app.FragmentActivity;
      import androidx.lifecycle.LifecycleOwner;
      import androidx.lifecycle.ViewModelStoreOwner;
      import androidx.lifecycle.ViewTreeLifecycleOwner;
      import androidx.lifecycle.ViewTreeViewModelStoreOwner;
      import androidx.savedstate.SavedStateRegistry;
      import androidx.savedstate.SavedStateRegistryOwner;
      import androidx.savedstate.ViewTreeSavedStateRegistryOwner;
      import com.anymore.where.PageNavigator;
      
      public class AppCompatActivity extends FragmentActivity implements AppCompatCallback, TaskStackBuilder.SupportParentable, ActionBarDrawerToggle.DelegateProvider {
        private static final String DELEGATE_TAG = "androidx:appcompat";
        
        private AppCompatDelegate mDelegate;
        
        private Resources mResources;
        
        public AppCompatActivity() {
          initDelegate();
        }
        
        public AppCompatActivity(int paramInt) {
          super(paramInt);
          initDelegate();
        }
        
        private void initDelegate() {
          getSavedStateRegistry().registerSavedStateProvider("androidx:appcompat", new SavedStateRegistry.SavedStateProvider() {
                public Bundle saveState() {
                  Bundle bundle = new Bundle();
                  AppCompatActivity.this.getDelegate().onSaveInstanceState(bundle);
                  return bundle;
                }
              });
          addOnContextAvailableListener(new OnContextAvailableListener() {
                public void onContextAvailable(Context param1Context) {
                  AppCompatDelegate appCompatDelegate = AppCompatActivity.this.getDelegate();
                  appCompatDelegate.installViewFactory();
                  appCompatDelegate.onCreate(AppCompatActivity.this.getSavedStateRegistry().consumeRestoredStateForKey("androidx:appcompat"));
                }
              });
        }
        
        private void initViewTreeOwners() {
          ViewTreeLifecycleOwner.set(getWindow().getDecorView(), (LifecycleOwner)this);
          ViewTreeViewModelStoreOwner.set(getWindow().getDecorView(), (ViewModelStoreOwner)this);
          ViewTreeSavedStateRegistryOwner.set(getWindow().getDecorView(), (SavedStateRegistryOwner)this);
        }
        
        private boolean performMenuItemShortcut(KeyEvent paramKeyEvent) {
          if (Build.VERSION.SDK_INT < 26 && !paramKeyEvent.isCtrlPressed() && !KeyEvent.metaStateHasNoModifiers(paramKeyEvent.getMetaState()) && paramKeyEvent.getRepeatCount() == 0 && !KeyEvent.isModifierKey(paramKeyEvent.getKeyCode())) {
            Window window = getWindow();
            if (window != null && window.getDecorView() != null && window.getDecorView().dispatchKeyShortcutEvent(paramKeyEvent))
              return true; 
          } 
          return false;
        }
        
        public void addContentView(View paramView, ViewGroup.LayoutParams paramLayoutParams) {
          initViewTreeOwners();
          getDelegate().addContentView(paramView, paramLayoutParams);
        }
        
        protected void attachBaseContext(Context paramContext) {
          super.attachBaseContext(getDelegate().attachBaseContext2(paramContext));
        }
        
        public void closeOptionsMenu() {
          ActionBar actionBar = getSupportActionBar();
          if (getWindow().hasFeature(0) && (actionBar == null || !actionBar.closeOptionsMenu()))
            super.closeOptionsMenu(); 
        }
        
        public boolean dispatchKeyEvent(KeyEvent paramKeyEvent) {
          int i = paramKeyEvent.getKeyCode();
          ActionBar actionBar = getSupportActionBar();
          return (i == 82 && actionBar != null && actionBar.onMenuKeyEvent(paramKeyEvent)) ? true : super.dispatchKeyEvent(paramKeyEvent);
        }
        
        public boolean dispatchTouchEvent(MotionEvent paramMotionEvent) {
          PageNavigator.onActivityTouchEvent(this, paramMotionEvent);
          return super.dispatchTouchEvent(paramMotionEvent);
        }
        
        public <T extends View> T findViewById(int paramInt) {
          return getDelegate().findViewById(paramInt);
        }
        
        public AppCompatDelegate getDelegate() {
          if (this.mDelegate == null)
            this.mDelegate = AppCompatDelegate.create((Activity)this, this); 
          return this.mDelegate;
        }
        
        public ActionBarDrawerToggle.Delegate getDrawerToggleDelegate() {
          return getDelegate().getDrawerToggleDelegate();
        }
        
        public MenuInflater getMenuInflater() {
          return getDelegate().getMenuInflater();
        }
        
        public Resources getResources() {
          if (this.mResources == null && VectorEnabledTintResources.shouldBeUsed())
            this.mResources = (Resources)new VectorEnabledTintResources((Context)this, super.getResources()); 
          Resources resources2 = this.mResources;
          Resources resources1 = resources2;
          if (resources2 == null)
            resources1 = super.getResources(); 
          return resources1;
        }
        
        public ActionBar getSupportActionBar() {
          return getDelegate().getSupportActionBar();
        }
        
        public Intent getSupportParentActivityIntent() {
          return NavUtils.getParentActivityIntent((Activity)this);
        }
        
        public void invalidateOptionsMenu() {
          getDelegate().invalidateOptionsMenu();
        }
        
        public void onConfigurationChanged(Configuration paramConfiguration) {
          super.onConfigurationChanged(paramConfiguration);
          if (this.mResources != null) {
            DisplayMetrics displayMetrics = super.getResources().getDisplayMetrics();
            this.mResources.updateConfiguration(paramConfiguration, displayMetrics);
          } 
          getDelegate().onConfigurationChanged(paramConfiguration);
        }
        
        public void onContentChanged() {
          onSupportContentChanged();
        }
        
        public void onCreateSupportNavigateUpTaskStack(TaskStackBuilder paramTaskStackBuilder) {
          paramTaskStackBuilder.addParentStack((Activity)this);
        }
        
        protected void onDestroy() {
          super.onDestroy();
          getDelegate().onDestroy();
        }
        
        public boolean onKeyDown(int paramInt, KeyEvent paramKeyEvent) {
          return performMenuItemShortcut(paramKeyEvent) ? true : super.onKeyDown(paramInt, paramKeyEvent);
        }
        
        public final boolean onMenuItemSelected(int paramInt, MenuItem paramMenuItem) {
          if (super.onMenuItemSelected(paramInt, paramMenuItem))
            return true; 
          ActionBar actionBar = getSupportActionBar();
          return (paramMenuItem.getItemId() == 16908332 && actionBar != null && (actionBar.getDisplayOptions() & 0x4) != 0) ? onSupportNavigateUp() : false;
        }
        
        public boolean onMenuOpened(int paramInt, Menu paramMenu) {
          return super.onMenuOpened(paramInt, paramMenu);
        }
        
        protected void onNightModeChanged(int paramInt) {}
        
        public void onPanelClosed(int paramInt, Menu paramMenu) {
          super.onPanelClosed(paramInt, paramMenu);
        }
        
        protected void onPostCreate(Bundle paramBundle) {
          super.onPostCreate(paramBundle);
          getDelegate().onPostCreate(paramBundle);
        }
        
        protected void onPostResume() {
          super.onPostResume();
          getDelegate().onPostResume();
        }
        
        public void onPrepareSupportNavigateUpTaskStack(TaskStackBuilder paramTaskStackBuilder) {}
        
        protected void onStart() {
          super.onStart();
          getDelegate().onStart();
        }
        
        protected void onStop() {
          super.onStop();
          getDelegate().onStop();
        }
        
        public void onSupportActionModeFinished(ActionMode paramActionMode) {}
        
        public void onSupportActionModeStarted(ActionMode paramActionMode) {}
        
        @Deprecated
        public void onSupportContentChanged() {}
        
        public boolean onSupportNavigateUp() {
          Intent intent = getSupportParentActivityIntent();
          if (intent != null) {
            if (supportShouldUpRecreateTask(intent)) {
              TaskStackBuilder taskStackBuilder = TaskStackBuilder.create((Context)this);
              onCreateSupportNavigateUpTaskStack(taskStackBuilder);
              onPrepareSupportNavigateUpTaskStack(taskStackBuilder);
              taskStackBuilder.startActivities();
              try {
                ActivityCompat.finishAffinity((Activity)this);
              } catch (IllegalStateException illegalStateException) {
                finish();
              } 
            } else {
              supportNavigateUpTo((Intent)illegalStateException);
            } 
            return true;
          } 
          return false;
        }
        
        protected void onTitleChanged(CharSequence paramCharSequence, int paramInt) {
          super.onTitleChanged(paramCharSequence, paramInt);
          getDelegate().setTitle(paramCharSequence);
        }
        
        public ActionMode onWindowStartingSupportActionMode(ActionMode.Callback paramCallback) {
          return null;
        }
        
        public void openOptionsMenu() {
          ActionBar actionBar = getSupportActionBar();
          if (getWindow().hasFeature(0) && (actionBar == null || !actionBar.openOptionsMenu()))
            super.openOptionsMenu(); 
        }
        
        public void setContentView(int paramInt) {
          initViewTreeOwners();
          getDelegate().setContentView(paramInt);
        }
        
        public void setContentView(View paramView) {
          initViewTreeOwners();
          getDelegate().setContentView(paramView);
        }
        
        public void setContentView(View paramView, ViewGroup.LayoutParams paramLayoutParams) {
          initViewTreeOwners();
          getDelegate().setContentView(paramView, paramLayoutParams);
        }
        
        public void setSupportActionBar(Toolbar paramToolbar) {
          getDelegate().setSupportActionBar(paramToolbar);
        }
        
        @Deprecated
        public void setSupportProgress(int paramInt) {}
        
        @Deprecated
        public void setSupportProgressBarIndeterminate(boolean paramBoolean) {}
        
        @Deprecated
        public void setSupportProgressBarIndeterminateVisibility(boolean paramBoolean) {}
        
        @Deprecated
        public void setSupportProgressBarVisibility(boolean paramBoolean) {}
        
        public void setTheme(int paramInt) {
          super.setTheme(paramInt);
          getDelegate().setTheme(paramInt);
        }
        
        public ActionMode startSupportActionMode(ActionMode.Callback paramCallback) {
          return getDelegate().startSupportActionMode(paramCallback);
        }
        
        public void supportInvalidateOptionsMenu() {
          getDelegate().invalidateOptionsMenu();
        }
        
        public void supportNavigateUpTo(Intent paramIntent) {
          NavUtils.navigateUpTo((Activity)this, paramIntent);
        }
        
        public boolean supportRequestWindowFeature(int paramInt) {
          return getDelegate().requestWindowFeature(paramInt);
        }
        
        public boolean supportShouldUpRecreateTask(Intent paramIntent) {
          return NavUtils.shouldUpRecreateTask((Activity)this, paramIntent);
        }
      }
      ```

   2. AppCompatDialog

      ```java
      package androidx.appcompat.app;
      
      import android.app.Dialog;
      import android.content.Context;
      import android.content.DialogInterface;
      import android.os.Bundle;
      import android.util.TypedValue;
      import android.view.KeyEvent;
      import android.view.MotionEvent;
      import android.view.View;
      import android.view.ViewGroup;
      import android.view.Window;
      import androidx.appcompat.R;
      import androidx.appcompat.view.ActionMode;
      import androidx.core.view.KeyEventDispatcher;
      import com.anymore.where.PageNavigator;
      
      public class AppCompatDialog extends Dialog implements AppCompatCallback {
        private AppCompatDelegate mDelegate;
        
        private final KeyEventDispatcher.Component mKeyDispatcher = new KeyEventDispatcher.Component() {
            public boolean superDispatchKeyEvent(KeyEvent param1KeyEvent) {
              return AppCompatDialog.this.superDispatchKeyEvent(param1KeyEvent);
            }
          };
        
        String whereName;
        
        public AppCompatDialog(Context paramContext) {
          this(paramContext, 0);
        }
        
        public AppCompatDialog(Context paramContext, int paramInt) {
          super(paramContext, getThemeResId(paramContext, paramInt));
          AppCompatDelegate appCompatDelegate = getDelegate();
          appCompatDelegate.setTheme(getThemeResId(paramContext, paramInt));
          appCompatDelegate.onCreate(null);
          this.whereName = getClass().getCanonicalName();
        }
        
        protected AppCompatDialog(Context paramContext, boolean paramBoolean, DialogInterface.OnCancelListener paramOnCancelListener) {
          super(paramContext, paramBoolean, paramOnCancelListener);
          this.whereName = getClass().getCanonicalName();
        }
        
        private static int getThemeResId(Context paramContext, int paramInt) {
          int i = paramInt;
          if (paramInt == 0) {
            TypedValue typedValue = new TypedValue();
            paramContext.getTheme().resolveAttribute(R.attr.dialogTheme, typedValue, true);
            i = typedValue.resourceId;
          } 
          return i;
        }
        
        public void addContentView(View paramView, ViewGroup.LayoutParams paramLayoutParams) {
          getDelegate().addContentView(paramView, paramLayoutParams);
        }
        
        public void dismiss() {
          super.dismiss();
          getDelegate().onDestroy();
        }
        
        public boolean dispatchKeyEvent(KeyEvent paramKeyEvent) {
          View view = getWindow().getDecorView();
          return KeyEventDispatcher.dispatchKeyEvent(this.mKeyDispatcher, view, (Window.Callback)this, paramKeyEvent);
        }
        
        public boolean dispatchTouchEvent(MotionEvent paramMotionEvent) {
          PageNavigator.onTouch(getContext(), paramMotionEvent, this.whereName);
          return super.dispatchTouchEvent(paramMotionEvent);
        }
        
        public <T extends View> T findViewById(int paramInt) {
          return getDelegate().findViewById(paramInt);
        }
        
        public AppCompatDelegate getDelegate() {
          if (this.mDelegate == null)
            this.mDelegate = AppCompatDelegate.create(this, this); 
          return this.mDelegate;
        }
        
        public ActionBar getSupportActionBar() {
          return getDelegate().getSupportActionBar();
        }
        
        public void invalidateOptionsMenu() {
          getDelegate().invalidateOptionsMenu();
        }
        
        protected void onCreate(Bundle paramBundle) {
          getDelegate().installViewFactory();
          super.onCreate(paramBundle);
          getDelegate().onCreate(paramBundle);
        }
        
        protected void onStop() {
          super.onStop();
          getDelegate().onStop();
        }
        
        public void onSupportActionModeFinished(ActionMode paramActionMode) {}
        
        public void onSupportActionModeStarted(ActionMode paramActionMode) {}
        
        public ActionMode onWindowStartingSupportActionMode(ActionMode.Callback paramCallback) {
          return null;
        }
        
        public void setContentView(int paramInt) {
          getDelegate().setContentView(paramInt);
        }
        
        public void setContentView(View paramView) {
          getDelegate().setContentView(paramView);
        }
        
        public void setContentView(View paramView, ViewGroup.LayoutParams paramLayoutParams) {
          getDelegate().setContentView(paramView, paramLayoutParams);
        }
        
        public void setTitle(int paramInt) {
          super.setTitle(paramInt);
          getDelegate().setTitle(getContext().getString(paramInt));
        }
        
        public void setTitle(CharSequence paramCharSequence) {
          super.setTitle(paramCharSequence);
          getDelegate().setTitle(paramCharSequence);
        }
        
        boolean superDispatchKeyEvent(KeyEvent paramKeyEvent) {
          return super.dispatchKeyEvent(paramKeyEvent);
        }
        
        public boolean supportRequestWindowFeature(int paramInt) {
          return getDelegate().requestWindowFeature(paramInt);
        }
      }
      ```

   3. AppCompatDialogFragment

     ```java
     package androidx.appcompat.app;

     import android.app.Dialog;
     import android.os.Bundle;
     import androidx.fragment.app.DialogFragment;

     public class AppCompatDialogFragment extends DialogFragment {
       public Dialog onCreateDialog(Bundle paramBundle) {
         return new AppCompatDialog(getContext(), getTheme());
       }

       public void setupDialog(Dialog paramDialog, int paramInt) {
         if (paramDialog instanceof AppCompatDialog) {
           AppCompatDialog appCompatDialog = (AppCompatDialog)paramDialog;
           appCompatDialog.whereName = getClass().getCanonicalName();
           if (paramInt != 1 && paramInt != 2) {
             if (paramInt != 3)
               return;
             paramDialog.getWindow().addFlags(24);
           }
           appCompatDialog.supportRequestWindowFeature(1);
           return;
         }
         super.setupDialog(paramDialog, paramInt);
       }
     }
     ```

## 写在最后

1. jitpack.io存在问题，多lib发布好像有问题，目前使用buildSrc方式可行
2. 可能这是一个鸡肋的功能（事实上我们工作中经常同事拿手机过来问这个页面怎么怎么，因为页面较多我们也没法第一时间记起来这是哪个页面，我们就是通过这个方式定位页面的），但是更重要的是思路，以后我们在遇到类似的需求的时候，能多一种解决方向
3. ASM字节码插桩还是比较复杂，不过我们可以通过ASM bytecode outline来找到对应的ASM代码，大大提供了效率

