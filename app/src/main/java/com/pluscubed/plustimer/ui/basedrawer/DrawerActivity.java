package com.pluscubed.plustimer.ui.basedrawer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.base.PresenterFactory;
import com.pluscubed.plustimer.ui.SettingsActivity;
import com.pluscubed.plustimer.ui.about.AboutActivity;
import com.pluscubed.plustimer.ui.currentsession.CurrentSessionActivity;
import com.pluscubed.plustimer.ui.historysessions.HistorySessionsActivity;
import com.pluscubed.plustimer.utils.PrefUtils;

/**
 * Base Activity with the Navigation Drawer
 */
public abstract class DrawerActivity<P extends DrawerPresenter<V>, V extends DrawerView> extends ThemableActivity<P, V> implements DrawerView{

    private static final int[] NAVDRAWER_TOOLBAR_TITLE_RES_ID = new int[]{
            R.string.current,
            R.string.history
    };
    private DrawerLayout mDrawerLayout;

    private Handler mHandler;
    private Toolbar mToolbar;
    private NavigationView mNavView;
    private ImageView mHeaderProfileImage;
    private TextView mHeaderTitle;
    private TextView mHeaderSubtitle;

    @Override
    public Activity getContextCompat() {
        return this;
    }

    protected abstract int getSelfNavDrawerItem();

    protected void onNavDrawerSlide(float offset) {
    }

    protected void onNavDrawerClosed() {
    }

    @Override
    protected boolean hasNavDrawer() {
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        presenter.onNewIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        presenter.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void displayToast(String message){
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    protected Toolbar getToolbar() {
        if (mToolbar == null) {
            mToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
            if (mToolbar != null) {
                setSupportActionBar(mToolbar);
            }
        }
        return mToolbar;
    }

    private Bitmap getCircleBitmap(Bitmap bitmap) {
        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.BLACK);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();

        return output;
    }

    @Override
    public void setProfileImage(String url){
        Glide.with(this)
                .load(url)
                .asBitmap()
                .centerCrop()
                .transform(new BitmapTransformation(this) {
                    @Override
                    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
                        return getCircleBitmap(toTransform);
                    }

                    @Override
                    public String getId() {
                        return "circle";
                    }
                })
                .into(mHeaderProfileImage);
    }

    @Override
    public void setHeaderText(String title, String subtitle){
        mHeaderTitle.setText(title);
        mHeaderSubtitle.setText(subtitle);
    }

    /**
     * Sets up the navigation drawer as appropriate.
     */
    private void setupNavDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id
                .activity_drawer_drawerlayout);
        Resources resources = getResources();

        mToolbar.setNavigationIcon(R.drawable.ic_drawer);
        mToolbar.setNavigationOnClickListener(view ->
                mDrawerLayout.openDrawer(GravityCompat.START));

        mDrawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                onNavDrawerSlide(slideOffset);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                onNavDrawerClosed();
            }
        });

        mNavView = (NavigationView) findViewById(R.id.activity_drawer_drawer_navview);
        mNavView.setCheckedItem(getSelfNavDrawerItem());
        mNavView.setNavigationItemSelectedListener(item -> {
            onNavDrawerItemClicked(item.getItemId(), item.getGroupId());

            return isNormalItem(item.getItemId());
        });



        View headerView = mNavView.getHeaderView(0);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            View inset = headerView.findViewById(R.id.inset);
            ViewGroup.LayoutParams params = inset.getLayoutParams();
            params.height = insets.getSystemWindowInsetTop();
            inset.setLayoutParams(params);
            return insets;
        });

        mHeaderProfileImage = (ImageView) headerView.findViewById(R.id.drawer_header_profile_image);
        VectorDrawableCompat drawable = VectorDrawableCompat.create(getResources(), R.drawable.profile_placeholder, getTheme());
        mHeaderProfileImage.setImageDrawable(drawable);
        mHeaderProfileImage.setOnClickListener(v -> presenter.onNavDrawerHeaderClicked());

        mHeaderTitle = (TextView) headerView.findViewById(R.id.drawer_header_title);
        mHeaderSubtitle = (TextView) headerView.findViewById(R.id.drawer_header_subtitle);

        View background = headerView.findViewById(R.id.drawer_header_background);
        background.setOnClickListener(v -> presenter.onNavDrawerHeaderClicked());

        int actionBarSize = resources.getDimensionPixelSize(R.dimen
                .navigation_drawer_margin);
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        int navDrawerWidthLimit = resources.getDimensionPixelSize(R.dimen.navigation_drawer_limit);
        int navDrawerWidth = displayMetrics.widthPixels - actionBarSize;
        if (navDrawerWidth > navDrawerWidthLimit) {
            navDrawerWidth = navDrawerWidthLimit;
        }
        DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) mNavView.getLayoutParams();
        params.width = navDrawerWidth;
        mNavView.setLayoutParams(params);

        resetTitle();

        if (!PrefUtils.isWelcomeDone(this)) {
            PrefUtils.markWelcomeDone(this);
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }

    @Override
    protected PresenterFactory<P> getPresenterFactory() {
        return new DrawerPresenter.Factory();
    }

    void resetTitle() {
        setTitle(NAVDRAWER_TOOLBAR_TITLE_RES_ID[getSelfNavDrawerItem() == R.id.nav_current ? 0 : 1]);
        final View root = findViewById(android.R.id.content);
        ViewTreeObserver vto = root.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                if (getToolbar().isTitleTruncated()) {
                    setTitle(null);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    root.getViewTreeObserver()
                            .removeOnGlobalLayoutListener(this);
                } else {
                    root.getViewTreeObserver()
                            .removeGlobalOnLayoutListener(this);
                }
            }
        });
    }

    protected void lockDrawer(boolean lock) {
        if (lock) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        getToolbar();
    }

    @Override
    public void onBackPressed() {
        if (isNavDrawerOpen()) {
            closeNavDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        resetTitle();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupNavDrawer();
    }

    private void onNavDrawerItemClicked(@IdRes int itemId, @IdRes int groupId) {
        if (itemId == getSelfNavDrawerItem()) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
        mHandler.postDelayed(() -> goToNavDrawerItem(itemId), 250);
    }

    private boolean isNormalItem(@IdRes int itemId) {
        return itemId != R.id.nav_settings && itemId != R.id.nav_about;
    }

    protected boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    void closeNavDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void goToNavDrawerItem(@IdRes int itemId) {
        Intent i;
        switch (itemId) {
            case R.id.nav_current:
                i = new Intent(this, CurrentSessionActivity.class);
                break;
            case R.id.nav_history:
                i = new Intent(this, HistorySessionsActivity.class);
                break;
            case R.id.nav_settings:
                i = new Intent(this, SettingsActivity.class);
                break;
            case R.id.nav_about:
                i = new Intent(this, AboutActivity.class);
                break;
            default:
                Toast.makeText(getApplicationContext(), "Work in Progress",
                        Toast.LENGTH_SHORT).show();
                return;
        }

        if (isNormalItem(itemId)) {
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        } else {
            startActivity(i);
        }
    }
}
