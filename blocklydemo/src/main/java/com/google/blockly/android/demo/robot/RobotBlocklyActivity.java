/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.blockly.android.demo.robot;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.google.blockly.android.AbstractBlocklyActivity;
import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.demo.R;
import com.google.blockly.android.demo.bleutils.BleController;
import com.google.blockly.android.demo.bleutils.callback.OnWriteCallback;
import com.google.blockly.android.demo.javainterface.ControlInterface;
import com.google.blockly.android.demo.javainterface.MoveInterface;
import com.google.blockly.android.demo.javainterface.ShowInterface;
import com.google.blockly.model.DefaultBlocks;
import com.google.blockly.util.JavascriptUtil;
import com.google.blockly.util.ToastUtils;
import com.google.blockly.utils.BlockLoadingException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class RobotBlocklyActivity extends AbstractBlocklyActivity {
    private MoveInterface moveInterface;
    private ShowInterface showInterface;
    private ControlInterface controlInterface;
    private long lastTime;
    private View mRun;

    public static void launch(Context context) {
        context.startActivity(new Intent(context, RobotBlocklyActivity.class));
    }

    private static final String TAG = "RobotBlocklyActivity";

    private static final String SAVE_FILENAME = "turtle_workspace.xml";
    private static final String AUTOSAVE_FILENAME = "turtle_workspace_temp.xml";

    public void end() {
        mRun.setEnabled(true);
    }

    public interface ReceiverListener {
        void onReceive(String s);
    }

    private ReceiverListener listener;

    public void setListener(ReceiverListener listener) {
        this.listener = listener;
    }

    static final List<String> TURTLE_BLOCK_DEFINITIONS = Arrays.asList(
            DefaultBlocks.COLOR_BLOCKS_PATH,
            DefaultBlocks.LOGIC_BLOCKS_PATH,
            DefaultBlocks.LOOP_BLOCKS_PATH,
            DefaultBlocks.MATH_BLOCKS_PATH,
            DefaultBlocks.TEXT_BLOCKS_PATH,
            DefaultBlocks.VARIABLE_BLOCKS_PATH,
            "robot/robot_blocks.json"
    );
    static final List<String> TURTLE_BLOCK_GENERATORS = Arrays.asList(
            "robot/generators.js"
    );


    private final Handler mHandler = new Handler();
    private WebView mWebview;
    private final CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
            new CodeGenerationRequest.CodeGeneratorCallback() {
                @Override
                public void onFinishCodeGeneration(final String generatedCode) {
                    Log.e(TAG, "generatedCode:\n" + generatedCode);
                    final String newStr = generatedCode;
                    String strs[] = newStr.split("\n\n");
                    if (strs == null || strs.length < 1) {
                        return;
                    }
                    for (int i = 0; i < strs.length; i++) {
                        if (strs[i].startsWith("option.command('option_start')")) {

                            int finalI = i;
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    String encoded = "Robot.execute("
                                            + JavascriptUtil.makeJsString(strs[finalI] + "option.command('option_end');") + ")";
                                    Log.e(TAG, "generatedCode:\n  " + strs[finalI] + "option.command('option_end');");
                                    Log.e(TAG, "encoded:\n  " +encoded);
                                    mWebview.loadUrl("javascript:" + encoded);
                                }
                            });
                            break;
                        }
                    }
                }
            };
    private BleController mBleController;

    @Override
    public void onLoadWorkspace() {
        mBlocklyActivityHelper.loadWorkspaceFromAppDirSafely(SAVE_FILENAME);
    }

    public void loadWorkPlace() {
        BlocklyController controller = getController();
        String filename = "";
        filename = "robot.xml";

        String assetFilename = "robot/" + filename;
        try {
            controller.loadWorkspaceContents(getAssets().open(assetFilename));
        } catch (IOException | BlockLoadingException e) {
            throw new IllegalStateException(
                    "Couldn't load demo workspace from assets: " + assetFilename, e);
        }
    }

    @Override
    public void onSaveWorkspace() {
        mBlocklyActivityHelper.saveWorkspaceToAppDirSafely(SAVE_FILENAME);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @NonNull
    @Override
    protected List<String> getBlockDefinitionsJsonPaths() {
        // Use the same blocks for all the levels. This lets the user's block code carry over from
        // level to level. The set of blocks shown in the toolbox for each level is defined by the
        // toolbox path below.
        return TURTLE_BLOCK_DEFINITIONS;
    }

    @Override
    protected int getActionBarMenuResId() {
        return R.menu.turtle_actionbar;
    }

    @NonNull
    @Override
    protected List<String> getGeneratorsJsPaths() {
        return TURTLE_BLOCK_GENERATORS;
    }

    @NonNull
    @Override
    protected String getToolboxContentsXmlPath() {
        // Expose a different set of blocks to the user at each level.
        return "robot/toolbox_basic_robot.xml";
    }

    @Override
    protected void onInitBlankWorkspace() {
        loadWorkPlace();
        addDefaultVariables(getController());
    }


    @Override
    protected View onCreateContentView(int parentId) {
        getSupportActionBar().hide();
        View root = getLayoutInflater().inflate(R.layout.turtle_content, null);
        root.findViewById(R.id.back).setOnClickListener(v -> finish());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mRun = root.findViewById(R.id.run);
        mRun.setOnClickListener(v -> {
            if (!BleController.getInstance().isConnected()) {
                RobotBleConnectActivity.launch(this);
                return;
            }
            v.setEnabled(false);
            run();
        });

        mWebview = (WebView) root.findViewById(R.id.turtle_runtime);
        mWebview.getSettings().setJavaScriptEnabled(true);
        mWebview.setWebChromeClient(new WebChromeClient());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        mWebview.loadUrl("file:///android_asset/robot/empty.html");
        //参数1：Javascript对象名
        //参数2：Java对象名
        moveInterface = new MoveInterface(this);
        showInterface = new ShowInterface(this);
        controlInterface = new ControlInterface(this);
        mWebview.addJavascriptInterface(moveInterface, "move");
        mWebview.addJavascriptInterface(showInterface, "option");
        mWebview.addJavascriptInterface(controlInterface, "control");
        System.out.println("thread" + Thread.currentThread().toString());
        //获得实例
        mBleController = BleController.getInstance();
        // TODO 接收数据的监听
        mBleController.RegistReciveListener(TAG, value -> {
            Log.e("response", new String(value));
            if (listener != null) {
                listener.onReceive(new String(value));
            }
        });

        mWebview.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                AlertDialog.Builder b = new AlertDialog.Builder(RobotBlocklyActivity.this);
                b.setTitle("Alert");
                b.setMessage(message);
                b.setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm());
                b.setCancelable(false);
                b.create().show();
                return true;
            }

        });
        return root;
    }

    private void run() {
        if (getController().getWorkspace().hasBlocks()) {
            onRunCode();
        } else {
            Log.i(TAG, "No blocks in workspace. Skipping run request.");
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void Write(String value) {
        Log.e(TAG, "send data：" + value);
        Log.e("pause", String.valueOf(System.currentTimeMillis() - lastTime));
        lastTime = System.currentTimeMillis();
        mBleController.WriteBuffer(value, new OnWriteCallback() {
            @Override
            public void onSuccess() {
//               ToastUtils.show("发送成功"); Toast thành công
            }

            @Override
            public void onFailed(int state) {
                ToastUtils.show("发送失败");
            } // Toast thất bại
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //移除接收数据的监听 Xóa nghe nhận dữ liệu
        mBleController.UnregistReciveListener(TAG);
    }

    @NonNull
    @Override
    protected CodeGenerationRequest.CodeGeneratorCallback getCodeGenerationCallback() {
        return mCodeGeneratorCallback;
    }

    static void addDefaultVariables(BlocklyController controller) {
        // TODO: (#22) Remove this override when variables are supported properly
        controller.addVariable("item");
        controller.addVariable("count");
        controller.addVariable("marshmallow");
        controller.addVariable("lollipop");
        controller.addVariable("kitkat");
        controller.addVariable("android");
    }

    /**
     * Optional override of the save path, since this demo Activity has multiple Blockly
     * configurations.
     *
     * @return Workspace save path used by this Activity.
     */
    @Override
    @NonNull
    protected String getWorkspaceSavePath() {
        return SAVE_FILENAME;
    }

    /**
     * Optional override of the auto-save path, since this demo Activity has multiple Blockly
     * configurations.
     *
     * @return Workspace auto-save path used by this Activity.
     */
    @Override
    @NonNull
    protected String getWorkspaceAutosavePath() {
        return AUTOSAVE_FILENAME;
    }
}
