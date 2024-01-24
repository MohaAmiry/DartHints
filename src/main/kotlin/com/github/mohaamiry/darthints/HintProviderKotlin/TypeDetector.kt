package com.github.mohaamiry.darthints.HintProviderKotlin

import com.github.mohaamiry.darthints.MyBundle
import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.psi.PsiFile
import com.jetbrains.lang.dart.DartBundle;
import com.jetbrains.lang.dart.DartElementType;
import com.jetbrains.lang.dart.DartLanguage;
import com.jetbrains.lang.dart.psi.*;
import com.jetbrains.lang.dart.psi.impl.DartArgumentsImpl;
import org.jetbrains.plugins.groovy.GroovyBundle

import org.jetbrains.plugins.groovy.codeInsight.hint.GrMethodDeclarationRangeHandler
import javax.swing.JComponent


class TypeDetector : InlayHintsProvider<TypeDetector.Settings>{

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Settings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        return HintCollector(editor);
    }



    data class Settings(var showInferredParameterTypes: Boolean = true, var showTypeParameterList: Boolean = true);

    override fun createSettings(): Settings = Settings();
    companion object {
        val ourKey: SettingsKey<TypeDetector.Settings> = SettingsKey("dart.parameter.hints");
    }
    override val key: SettingsKey<TypeDetector.Settings>
        get() = TypeDetector.ourKey


    fun getLang() : Language = DartLanguage.INSTANCE;
    override val name: String
        get() = MyBundle.message("dart.settings.inlay.hints.name");


    override val previewText: String?
        get() = "this is previewText"

    override fun createConfigurable(settings: Settings): ImmediateConfigurable {
        TODO();
    }

    override val isVisibleInSettings: Boolean
        get() = true

    override fun isLanguageSupported(language: Language): Boolean {
        return super.isLanguageSupported(getLang())
    }
}
