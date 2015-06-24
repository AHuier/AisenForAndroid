package com.m.ui.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;

import com.m.R;
import com.m.common.utils.ActivityHelper;
import com.m.common.utils.Logger;
import com.m.support.adapter.FragmentPagerAdapter;
import com.m.support.inject.ViewInject;
import com.m.ui.activity.basic.BaseActivity;
import com.m.ui.fragment.AStripTabsFragment.IStripTabInitData;
import com.m.ui.fragment.AStripTabsFragment.StripTabItem;
import com.m.ui.widget.SlidingTabLayout;

/**
 * Created by wangdan on 15-1-20.
 */
public abstract class ATabLayoutFragment<T extends StripTabItem> extends ABaseFragment
                                implements ViewPager.OnPageChangeListener {

    static final String TAG = ATabLayoutFragment.class.getSimpleName();

    public static final String SET_INDEX = "com.m.ui.SET_INDEX";// 默认选择第几个

    @ViewInject(idStr = "tabLayout")
    TabLayout tabLayout;
    @ViewInject(idStr = "pager")
    ViewPager viewPager;
    MyViewPagerAdapter mViewPagerAdapter;

    ArrayList<T> mItems;
    Map<String, Fragment> fragments;
    int mCurrentPosition = 0;

    @Override
    protected int inflateContentView() {
        return R.layout.comm_ui_tablayout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        mCurrentPosition = viewPager.getCurrentItem();
        outState.putSerializable("items", mItems);
        outState.putInt("current", mCurrentPosition);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mItems = savedInstanceState == null ? generateTabs()
                                            : (ArrayList<T>) savedInstanceState.getSerializable("items");

        mCurrentPosition = savedInstanceState == null ? 0
                                                      : savedInstanceState.getInt("current");
    }

    @Override
    protected void layoutInit(LayoutInflater inflater, final Bundle savedInstanceSate) {
        super.layoutInit(inflater, savedInstanceSate);

        setHasOptionsMenu(true);

        if (delayGenerateTabs() == 0) {
            setTab(savedInstanceSate);
        }
        else {
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    setTab(savedInstanceSate);
                }

            }, delayGenerateTabs());
        }
    }

    protected void setTab(final Bundle savedInstanceSate) {
        if (getActivity() == null)
            return;

        if (savedInstanceSate == null) {
            if (getArguments() != null && getArguments().containsKey(SET_INDEX)) {
                mCurrentPosition = Integer.parseInt(getArguments().getSerializable(SET_INDEX).toString());
            }
            else {
                if (configLastPositionKey() != null) {
                    // 记录了最后阅读的标签
                    String type = ActivityHelper.getShareData("PagerLastPosition" + configLastPositionKey(), "");
                    if (!TextUtils.isEmpty(type)) {
                        for (int i = 0; i < mItems.size(); i++) {
                            StripTabItem item = mItems.get(i);
                            if (item.getType().equals(type)) {
                                mCurrentPosition = i;
                                break;
                            }
                        }
                    }
                }
            }
        }

        Logger.w("strip-current-" + mCurrentPosition);

        fragments = new HashMap<String, Fragment>();

        if (mItems == null)
            return;

        for (int i = 0; i < mItems.size(); i++) {
            Fragment fragment = getActivity().getFragmentManager().findFragmentByTag(makeFragmentName(i));
            if (fragment != null) {
                getActivity().getFragmentManager().beginTransaction()
                        .remove(fragment).commit();
            }
//                fragments.put(makeFragmentName(i), fragment);
        }

        mViewPagerAdapter = new MyViewPagerAdapter(getFragmentManager());
//					viewPager.setOffscreenPageLimit(mViewPagerAdapter.getCount());
        viewPager.setOffscreenPageLimit(0);
        viewPager.setAdapter(mViewPagerAdapter);
        if (mCurrentPosition >= mViewPagerAdapter.getCount())
            mCurrentPosition = 0;
        viewPager.setCurrentItem(mCurrentPosition);
        viewPager.addOnPageChangeListener(this);

        tabLayout.setupWithViewPager(viewPager);
    }

    protected void destoryFragments() {
        if (getActivity() != null) {
            if (getActivity() instanceof BaseActivity) {
                BaseActivity mainActivity = (BaseActivity) getActivity();
                if (mainActivity.mIsDestoryed())
                    return;
            }

            try {
                FragmentTransaction trs = getFragmentManager().beginTransaction();
                Set<String> keySet = fragments.keySet();
                for (String key : keySet) {
                    if (fragments.get(key) != null) {
                        trs.remove(fragments.get(key));

                        Logger.e("remove fragment , key = " + key);
                    }
                }
                trs.commit();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {

    }

    @Override
    public void onPageSelected(int position) {
        mCurrentPosition = position;

        if (configLastPositionKey() != null) {
            ActivityHelper.putShareData("PagerLastPosition" + configLastPositionKey(), mItems.get(position).getType());
        }

        // 查看是否需要拉取数据
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof IStripTabInitData) {
            ((IStripTabInitData) fragment).onStripTabRequestData();
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    protected String makeFragmentName(int position) {
        return mItems.get(position).getTitle();
    }

    // 是否保留最后阅读的标签
    protected String configLastPositionKey() {
        return null;
    }

    abstract protected ArrayList<T> generateTabs();

    abstract protected Fragment newFragment(T bean);

    // 延迟一点初始化tabs，用于在首页切换菜单的时候，太多的tab页导致有点点卡顿
    protected int delayGenerateTabs() {
        return 0;
    }

    @Override
    public void onDestroy() {
        try {
            destoryFragments();

            viewPager.setAdapter(null);
            mViewPagerAdapter = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

    public Fragment getCurrentFragment() {
        if (mViewPagerAdapter == null || mViewPagerAdapter.getCount() < mCurrentPosition)
            return null;

        return fragments.get(makeFragmentName(mCurrentPosition));
    }

    public Fragment getFragment(String tabTitle) {
        if (fragments == null || TextUtils.isEmpty(tabTitle))
            return null;

        for (int i = 0; i < mItems.size(); i++) {
            if (tabTitle.equals(mItems.get(i).getTitle())) {
                return fragments.get(makeFragmentName(i));
            }
        }

        return null;
    }

    public Map<String, Fragment> getFragments() {
        return fragments;
    }

    public ViewPager getViewPager() {
        return viewPager;
    }

    public TabLayout getTabLayout() {
        return tabLayout;
    }

    class MyViewPagerAdapter extends FragmentPagerAdapter {

        public MyViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = fragments.get(makeFragmentName(position));
            if (fragment == null) {
                fragment = newFragment(mItems.get(position));

                fragments.put(makeFragmentName(position), fragment);
            }

            return fragment;
        }

        @Override
        protected void freshUI(Fragment fragment) {
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mItems.get(position).getTitle();
        }

        @Override
        protected String makeFragmentName(int position) {
            return ATabLayoutFragment.this.makeFragmentName(position);
        }

    }

}
