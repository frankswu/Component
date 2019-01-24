package com.ehi.componentdemo.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ehi.base.ModuleConfig;
import com.ehi.base.interceptor.DialogShowInterceptor;
import com.ehi.component.impl.EHiRouter;
import com.ehi.component.impl.EHiRouterResult;
import com.ehi.component.impl.EHiRxRouter;
import com.ehi.component.support.EHiCallbackAdapter;
import com.ehi.componentdemo.R;

import io.reactivex.functions.Consumer;

/**
 * time   : 2018/12/27
 *
 * @author : xiaojinzi 30212
 */
public class TestFragmentRouterFragment extends Fragment implements View.OnClickListener {

    private TextView tv_detail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.test_fragment_router_frag, null);
        contentView.findViewById(R.id.normalJump).setOnClickListener(this);
        contentView.findViewById(R.id.rxJumpGetData).setOnClickListener(this);
        contentView.findViewById(R.id.testCallbackAfterFinish).setOnClickListener(this);
        contentView.findViewById(R.id.bt_clearInfo).setOnClickListener(this);
        tv_detail = contentView.findViewById(R.id.tv_detail);
        return contentView;
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.rxJumpGetData) {
            rxJumpGetData();
        } else if (viewId == R.id.normalJump) {
            normalJump();
        } else if (viewId == R.id.testCallbackAfterFinish) {
            testCallbackAfterFinish();
        } else if (viewId == R.id.bt_clearInfo) {
            tv_detail.setText("");
        }
    }

    private void addInfo(@Nullable EHiRouterResult routerResult, @Nullable Exception error, @NonNull String url, @Nullable Integer requestCode) {
        if (requestCode == null) {
            if (routerResult != null) {
                tv_detail.setText(tv_detail.getText() + "\n\n普通跳转成功,目标:" + url);
            } else {
                tv_detail.setText(tv_detail.getText() + "\n\n普通跳转失败,目标:" + url + ",error = " + error.getClass().getSimpleName() + " ,errorMsg = " + error.getMessage());
            }
        } else {
            if (routerResult != null) {
                tv_detail.setText(tv_detail.getText() + "\n\nRequestCode=" + requestCode + "普通跳转成功,目标:" + url);
            } else {
                tv_detail.setText(tv_detail.getText() + "\n\nRequestCode=" + requestCode + "普通跳转失败,目标:" + url + ",error = " + error.getClass().getSimpleName() + " ,errorMsg = " + error.getMessage());
            }
        }
    }

    private void rxJumpGetData(){
        EHiRxRouter
                .withFragment(this)
                .host("component1")
                .path("test")
                .query("data", "rxJumpGetData")
                .requestCode(456)
                .intentCall()
                .subscribe(new Consumer<Intent>() {
                    @Override
                    public void accept(Intent intent) throws Exception {
                        tv_detail.setText(tv_detail.getText() + "\n\nrequestCode=456,目标:component1/test?data=rxJumpGetData,获取目标页面数据成功啦：Data = " + intent.getStringExtra("data"));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        tv_detail.setText(tv_detail.getText() + "\n\nrequestCode=456,目标:component1/test?data=rxJumpGetData,获取目标页面数据失败,error = " + throwable.getClass().getSimpleName() + " ,errorMsg = " + throwable.getMessage());
                    }
                });
    }

    private void normalJump() {
        EHiRouter
                .withFragment(this)
                .host("component1")
                .path("test")
                .query("data", "normalJump")
                .putString("name", "cxj1")
                .putInt("age", 25)
                .navigate(new EHiCallbackAdapter() {
                    @Override
                    public void onSuccess(@NonNull EHiRouterResult result) {
                        addInfo(result, null, "component1/test?data=normalJump", null);
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        addInfo(null, error, "component1/test?data=normalJump", null);
                    }
                });
    }

    public void testCallbackAfterFinish() {

        EHiRxRouter
                .withFragment(this)
                .host(ModuleConfig.System.NAME)
                .path(ModuleConfig.System.CALL_PHONE)
                .interceptors(DialogShowInterceptor.class)
                .navigate(new EHiCallbackAdapter(){
                    @Override
                    public void onEvent(@Nullable EHiRouterResult result, @Nullable Exception error) {
                        super.onEvent(result, error);
                    }

                    @Override
                    public void onCancel() {
                        super.onCancel();
                    }
                });

        getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();

    }

}