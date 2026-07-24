package com.shiro.yosugahub.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.ui.theme.TermGreen
import com.shiro.yosugahub.ui.theme.TermLine
import com.shiro.yosugahub.ui.theme.TermRed
import com.shiro.yosugahub.ui.theme.TermText
import com.shiro.yosugahub.ui.theme.TermTextDim

/**
 * 端末風の入力欄(v5 UI)。Material の浮くラベルをやめ、`> ` プロンプト + プレースホルダで
 * 「コマンドを打ち込む」見た目にする。等幅・直角・緑。
 *
 * [label] を渡すと**欄の上に固定の見出し**を出す。入力すると消えてしまう Material の
 * 浮くラベルと違い、項目の多いフォームでも「どの欄が何か」が最後まで残る。
 */
@Composable
fun TerminalField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    singleLine: Boolean = false,
    isError: Boolean = false,
    /** 欄の下に出す補足・エラー文。エラー時は赤。 */
    supportingText: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isError) TermRed else TermTextDim,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            singleLine = singleLine,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            textStyle = MaterialTheme.typography.bodyMedium,
            leadingIcon = {
                Text("> ", color = TermGreen, style = MaterialTheme.typography.bodyMedium)
            },
            placeholder = {
                if (placeholder.isNotBlank()) {
                    Text(
                        placeholder,
                        color = TermTextDim,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TermGreen,
                unfocusedBorderColor = TermLine,
                errorBorderColor = TermRed,
                focusedTextColor = TermText,
                unfocusedTextColor = TermText,
                errorTextColor = TermText,
                cursorColor = TermGreen,
                focusedLeadingIconColor = TermGreen,
                unfocusedLeadingIconColor = TermGreen,
                errorLeadingIconColor = TermRed,
            ),
        )
        if (supportingText.isNotBlank()) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) TermRed else TermTextDim,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
