package com.shiro.yosugahub.ui.screen.records

import com.shiro.yosugahub.domain.model.Directive
import com.shiro.yosugahub.domain.model.DirectiveStatus
import com.shiro.yosugahub.domain.model.Project

/** 記録タブ「指示」セクションの並び替え・絞り込み(純粋関数)。 */

/**
 * 表示順: **配信中を先に**(対応が必要なものが上)、その中は新しい順。
 * 対応済みは記録として下に続く。
 */
fun sortDirectivesForDisplay(directives: List<Directive>): List<Directive> =
    directives.sortedWith(
        compareByDescending<Directive> { it.status == DirectiveStatus.OPEN }
            .thenByDescending { it.createdAt }
    )

/** プロジェクトで絞り込む。null は「すべて」。 */
fun filterDirectivesByProject(directives: List<Directive>, projectId: String?): List<Directive> =
    if (projectId == null) directives else directives.filter { it.projectId == projectId }

/**
 * 宛先の表示名。プロジェクトが見つからなければ projectId をそのまま出す
 * (プロジェクトを消しても指示書の宛先が空欄にならないように)。
 */
fun directiveTargetName(directive: Directive, projects: List<Project>): String =
    projects.firstOrNull { it.id == directive.projectId }?.name ?: directive.projectId

/** 指示書に実際に使われている宛先だけを、プロジェクト一覧の順で返す。 */
fun directiveTargetsPresent(directives: List<Directive>, projects: List<Project>): List<Project> =
    projects.filter { project -> directives.any { it.projectId == project.id } }
