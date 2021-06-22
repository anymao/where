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