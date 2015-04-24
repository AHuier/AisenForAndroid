package com.example.aisensample.ui.fragment;

import android.app.Fragment;

import com.example.aisensample.R;
import com.example.aisensample.support.bean.MenuBean;
import com.m.network.task.TaskException;
import com.m.ui.fragment.ABaseFragment;

/**
 * Created by wangdan on 15/4/23.
 */
public class ABaseSample extends ABaseFragment {

    public static Fragment newInstance() {
        return new ABaseSample();
    }

    @Override
    protected int inflateContentView() {
        return R.layout.ui_a_base;
    }

    @Override
    public void requestData() {
        super.requestData();

        new BaseTask().execute();
    }

    class BaseTask extends ABaseTask<Void, Void, MenuBean> {

        BaseTask() {
            super("ABaseFragment");
        }

        @Override
        public MenuBean workInBackground(Void... params) throws TaskException {
            int taskCount = getTaskCount(getTaskId());

            // 方便我截图，延迟一下
            try {
                long time = taskCount == 1 ? 5 : 2;

                Thread.sleep(time * 1000);
            } catch (Exception e) {
            }

            // 第一次，抛异常
            if (taskCount == 1) {
                throw new TaskException("0", "出现异常了");
            }
            // 第二次，数据为空
            else if (taskCount == 2) {
                setContentEmpty(true);
                return null;
            }
            // 第三次，展示正常数据
            else if (taskCount == 3) {
                setContentEmpty(false);
            }

            return new MenuBean();
        }

    }

}
