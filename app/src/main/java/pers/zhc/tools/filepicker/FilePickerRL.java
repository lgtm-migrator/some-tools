package pers.zhc.tools.filepicker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.tools.views.SmartHintEditText;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static pers.zhc.tools.utils.Common.showException;

/**
 * @author bczhc
 * Shitcode!
 */
@SuppressLint("ViewConstructor")
public class FilePickerRL extends RelativeLayout {
    public static final int TYPE_PICK_FILE = 1;
    public static final int TYPE_PICK_FOLDER = 2;
    private final File initialPath;
    private final OnCancelCallback cancelAction;
    private final OnPickedResultCallback pickedResultAction;
    private final int[] justPicked = new int[]{-1};
    private final @DrawableRes
    int cannotPick = R.drawable.file_picker_view_cannot_pick;
    private final @DrawableRes
    int canPickUnchecked = R.drawable.file_picker_view_can_pick_unchecked;
    private final @DrawableRes
    int viewChecked = R.drawable.file_picker_view_checked;
    private final int type;
    private final AppCompatActivity ctx;
    private String result;
    private TextView pathView;
    private File currentPath;
    private LinearLayout ll;
    private LinearLayout.LayoutParams lp;
    private EditText filterET;
    private @DrawableRes
    int unselectedDrawable;
    private CheckBox regexCB;
    private @Nullable
    final EditText filenameET;
    private RelativeLayout rootView;

    // Shitcode!!!
    public FilePickerRL(Context context, int type, @Nullable File initialPath
            , OnCancelCallback cancelAction, OnPickedResultCallback pickedResultAction
            , @Nullable String initFileName, boolean enableFilenameET) {
        super(context);
        this.ctx = (AppCompatActivity) context;
        this.type = type;
        this.initialPath = initialPath;
        this.cancelAction = cancelAction;
        this.pickedResultAction = pickedResultAction;
        initFileName = initFileName == null ? "" : initFileName;
        init(initFileName);

        final SmartHintEditText smartHintEditText = rootView.findViewById(R.id.filename_et);
        filenameET = smartHintEditText.getEditText();
        if (!enableFilenameET) {
            filenameET.setVisibility(GONE);
            filenameET.requestLayout();
        }
    }

    public FilePickerRL(Context context, int type, @Nullable File initialPath
            , OnCancelCallback cancelAction, OnPickedResultCallback pickedResultAction
            , @Nullable String initFileName) {
        this(context, type, initialPath, cancelAction, pickedResultAction, initFileName, false);
    }

    private void init(String initFileName) {
        this.unselectedDrawable = this.type == TYPE_PICK_FILE ? cannotPick : canPickUnchecked;
        rootView = ((RelativeLayout) View.inflate(ctx, R.layout.file_picker_rl_activity, null).getRootView());
        this.addView(rootView);
        this.lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        this.currentPath = initialPath == null ? new File(Common.getExternalStoragePath(ctx)) : initialPath;
        regexCB = findViewById(R.id.regex_cb);
        regexCB.setOnCheckedChangeListener((buttonView, isChecked) -> {
            File[] fileList = getFileList(FilePickerRL.this.currentPath);
            fillViews(fileList);
        });
        filterET = findViewById(R.id.filter_et);
        filterET.setText(initFileName);
        filterET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                File[] fileList = getFileList(FilePickerRL.this.currentPath);
                fillViews(fileList);
            }
        });
        Button cancel = findViewById(R.id.cancel);
        Button ok = findViewById(R.id.pick);
        cancel.setOnClickListener(v -> {
            result = null;
            cancelAction.cancel(this);
        });
        ok.setOnClickListener(v -> {
            if (filenameET.getVisibility() != GONE && filenameET.getText().toString().isEmpty()) {
                return;
            }
            if (type == 2) {
                String dir;
                dir = currentPath.getAbsolutePath();
                result = dir;
            }
            if (result != null)
                pickedResultAction.result(this, result);
        });
        this.pathView = findViewById(R.id.textView);
        this.pathView.setOnClickListener((v) -> {
            MaterialAlertDialogBuilder ad = new MaterialAlertDialogBuilder(ctx);
            EditText et = new EditText(ctx);
            String s = pathView.getText().toString();
            ctx.runOnUiThread(() -> et.setText(ctx.getString(R.string.str, ("/storage/emulated/".equals(s) ? s + "0" : s))));
            et.setSelection(et.getText().length());
            et.setLayoutParams(lp);
            AlertDialog alertDialog = ad.setTitle(R.string.type_path)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        String etText = et.getText().toString();
                        File f = new File(etText);
                        if (f.isFile() && type == 1) {
                            result = f.getAbsolutePath();
                            ok.performClick();
                        } else {
                            result = null;
                            File[] listFiles = getFileList(f);
                            this.currentPath = f;
                            fillViews(listFiles);
                        }
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    })
                    .setView(et).create();
            DialogUtil.setDialogAttr(alertDialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, null);
            DialogUtil.setAlertDialogWithEditTextAndAutoShowSoftKeyBoard(et, alertDialog);
            alertDialog.show();
        });
        this.ll = findViewById(R.id.ll);
        new Thread(() -> {
            File[] listFiles = getFileList(currentPath);
            fillViews(listFiles);
        }).start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void fillViews(File[] listFilesP) {
        final File[][] listFiles = {listFilesP};
        Thread thread = new Thread(() -> {
            listFiles[0] = filter(listFiles[0], filterET.getText().toString());
            justPicked[0] = -1;
            ctx.runOnUiThread(ll::removeAllViews);
            String currentPathString;
            try {
                currentPathString = currentPath.getCanonicalPath();
            } catch (IOException ignored) {
                currentPathString = currentPath.getAbsolutePath();
            }
            String finalCurrentPathString = currentPathString;
            ctx.runOnUiThread(() -> pathView.setText(ctx.getString(R.string.str
                    , result == null ? (finalCurrentPathString + ("/".equals(finalCurrentPathString) ? "" : File.separatorChar)) : result)));
            TextViewWithExtra[] textViews;
            int length = 0;
            try {
                length = listFiles[0].length;
            } catch (Exception e) {
                ctx.runOnUiThread(() -> ToastUtils.show(ctx, R.string.no_access));
                e.printStackTrace();
            }
            textViews = new TextViewWithExtra[length];
            switch (type) {
                case TYPE_PICK_FILE:
                    for (int i = 0; i < length; i++) {
                        final int finalI = i;
                        extractM1(listFiles[0], lp, textViews, i);
                        textViews[i].setOnClickListener(v -> {
                            File currentFile = listFiles[0][finalI];
                            if (currentFile.isFile()) {
                                if (textViews[finalI].picked) {
                                    textViews[finalI].setBackgroundResource(canPickUnchecked);
                                    result = null;
                                } else {
                                    try {
                                        if (justPicked[0] != -1) {
                                            textViews[justPicked[0]].setBackgroundResource(canPickUnchecked);
                                        }
                                        textViews[finalI].setBackgroundResource(viewChecked);
                                        justPicked[0] = finalI;
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        showException(e, ctx);
                                    }
                                    try {
                                        result = listFiles[0][justPicked[0]].getAbsolutePath();
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        showException(e, ctx);
                                    }
                                }
                            } else {
                                this.currentPath = currentFile;
                                File[] listFiles1 = getFileList(currentFile);
                                fillViews(listFiles1);
                                result = null;
                            }
                            ctx.runOnUiThread(() -> pathView.setText(ctx.getString(R.string.str
                                    , result == null ? (finalCurrentPathString + ("/".equals(finalCurrentPathString) ? "" : File.separatorChar)) : result)));
                        });
                        ctx.runOnUiThread(() -> ll.addView(textViews[finalI]));
                    }
                    break;
                case TYPE_PICK_FOLDER:
                    for (int i = 0; i < length; i++) {
                        extractM1(listFiles[0], lp, textViews, i);
                        final int finalI = i;
                        textViews[i].setOnClickListener(v -> {
                            File currentFile = listFiles[0][finalI];
                            if (currentFile.isDirectory()) {
                                this.currentPath = currentFile;
                                File[] listFiles1 = getFileList(currentFile);
                                fillViews(listFiles1);
                            }
                        });
                        ctx.runOnUiThread(() -> ll.addView(textViews[finalI]));
                    }
                    break;
                default:
                    break;
            }
        });
        thread.start();
    }

    private void extractM1(File[] listFiles, LinearLayout.LayoutParams lp, TextViewWithExtra[] textViews,
                           int i) {
        textViews[i] = new TextViewWithExtra(ctx);
        textViews[i].setBackgroundResource(R.drawable.view_stroke);
        textViews[i].setTextSize(25);
        ctx.runOnUiThread(() -> textViews[i].setText(listFiles[i].isFile() ? listFiles[i].getName() : (listFiles[i].getName())));
        textViews[i].setLayoutParams(lp);
        switch (type) {
            case TYPE_PICK_FILE:
                if (listFiles[i].isFile()) {
                    textViews[i].setBackgroundResource(canPickUnchecked);
                } else {
                    textViews[i].setBackgroundResource(unselectedDrawable);
                }
                break;
            case TYPE_PICK_FOLDER:
                if (listFiles[i].isFile()) {
                    textViews[i].setBackgroundResource(cannotPick);
                } else {
                    textViews[i].setBackgroundResource(canPickUnchecked);
                }
                break;
            default:
                break;
        }
    }

    public void previous() {
        result = null;
        if (currentPath.equals(new File(File.separator))) {
            cancelAction.cancel(this);
            return;
        }
        try {
            File parentFile = this.currentPath.getParentFile();
            File[] listFiles = new File[0];
            if (parentFile != null) {
                listFiles = getFileList(parentFile);
            }
            fillViews(listFiles);
            this.currentPath = parentFile;
        } catch (Exception e) {
            showException(e, ctx);
        }
    }

    private File[] getFileList(File file) {
        File[] files = file.listFiles();
        List<File> fileList = new LinkedList<>();
        List<File> dirList = new LinkedList<>();
        Collator stringComparator = Collator.getInstance(Locale.CHINA);
        Comparator<File> comparator = (o1, o2) -> stringComparator.compare(o1.getName(), o2.getName());
        if (files == null) {
            files = new File[0];
        }
        Arrays.sort(files, comparator);
        for (File f : files) {
            if (f.isDirectory()) {
                dirList.add(f);
            } else {
                fileList.add(f);
            }
        }
        List<File> r = new LinkedList<>();
        r.addAll(dirList);
        r.addAll(fileList);
        return r.toArray(new File[0]);
    }

    private File[] filter(File[] files, String filterStr) {
        boolean useRegExp = regexCB.isChecked();
        Pattern pattern = null;
        if (useRegExp) {
            try {
                pattern = Pattern.compile(filterStr);
                ctx.runOnUiThread(() -> filterET.setBackgroundResource(R.drawable.edittext_right));
            } catch (PatternSyntaxException ignored) {
                ctx.runOnUiThread(() -> filterET.setBackgroundResource(R.drawable.edittext_wrong));
            }
        } else ctx.runOnUiThread(() -> filterET.setBackgroundResource(R.drawable.edittext_right));
        List<File> fileList = new LinkedList<>();
        for (File file : files) {
            if (filterStr.isEmpty()) {
                fileList.add(file);
            } else {
                boolean match = true;
                if (useRegExp) {
                    if (pattern != null /* compile succeeded */) {
                        Matcher matcher = pattern.matcher(file.getName());
                        match = matcher.matches();
                    }
                } else {
                    match = file.getName().contains(filterStr);
                }
                if (match) {
                    fileList.add(file);
                }
            }
        }
        return fileList.toArray(new File[0]);
    }

    public interface OnPickedResultCallback {
        /**
         * onPickedResult callback
         *
         * @param picker this picker
         * @param path   result string
         */
        void result(FilePickerRL picker, String path);
    }

    private static class TextViewWithExtra extends AppCompatTextView {
        private boolean picked = false;

        public TextViewWithExtra(Context context) {
            super(context);


        }

        @Override
        public void setBackgroundResource(int resId) {
            super.setBackgroundResource(resId);
            this.picked = (resId == R.drawable.file_picker_view_checked);
        }
    }

    @Nullable
    public String getFilenameText() {
        if (filenameET == null) return null;
        return filenameET.getText().toString();
    }

    @Nullable
    public EditText getFilenameET() {
        return filenameET;
    }

    public interface OnCancelCallback {
        void cancel(@NotNull FilePickerRL picker);
    }
}
