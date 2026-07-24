package com.shiro.yosugahub.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import com.shiro.yosugahub.ui.theme.TermGreen
import com.shiro.yosugahub.ui.theme.TermLine
import com.shiro.yosugahub.ui.theme.TermText
import com.shiro.yosugahub.ui.theme.TermTextDim

/**
 * 端末風の入力欄(v5 UI)。Material の浮くラベルをやめ、`> ` プロンプト + プレースホルダで
 * 「コマンドを打ち込む」見た目にする。等幅・直角・緑。
 */
@Composable
fun TerminalField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        textStyle = MaterialTheme.typography.bodyMedium,
        leadingIcon = { Text("> ", color = TermGreen, style = MaterialTheme.typography.bodyMedium) },
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
            focusedTextColor = TermText,
            unfocusedTextColor = TermText,
            cursorColor = TermGreen,
            focusedLeadingIconColor = TermGreen,
            unfocusedLeadingIconColor = TermGreen,
        ),
    )
}
