package com.soywiz.korge.intellij.filetype

import com.intellij.lang.*
import com.intellij.lang.ParserDefinition.SpaceRequirements
import com.intellij.lang.xml.XMLLanguage
import com.intellij.lang.xml.XMLParserDefinition
import com.intellij.lexer.Lexer
import com.intellij.lexer.XmlLexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.xml.XmlFileImpl
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.xml.*

class TmxParserDefinition : ParserDefinition {

    override fun createLexer(project: Project): Lexer {
        return XmlLexer()
    }

    override fun getFileNodeType(): IFileElementType {
        return TmxFile.TMX_FILE_ELEMENT_TYPE
    }

    override fun getWhitespaceTokens(): TokenSet {
        return LanguageParserDefinitions.INSTANCE.forLanguage(
            Language.findInstance(XMLLanguage::class.java)
        ).whitespaceTokens
    }

    override fun getCommentTokens(): TokenSet {
        return LanguageParserDefinitions.INSTANCE.forLanguage(
            Language.findInstance(XMLLanguage::class.java)
        ).commentTokens
    }

    override fun getStringLiteralElements(): TokenSet {
        return TokenSet.EMPTY
    }

    override fun createParser(project: Project): PsiParser {
        return LanguageParserDefinitions.INSTANCE.forLanguage(
            Language.findInstance(XMLLanguage::class.java)
        ).createParser(project)
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return TmxFile(viewProvider)
    }

    override fun spaceExistenceTypeBetweenTokens(
        left: ASTNode,
        right: ASTNode
    ): SpaceRequirements = canStickTokensTogether(left, right)

    private fun canStickTokensTogether(left: ASTNode, right: ASTNode): SpaceRequirements = when {
        left.elementType === XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN || right.elementType === XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN -> SpaceRequirements.MUST_NOT
        left.elementType === XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER && right.elementType === XmlTokenType.XML_NAME -> SpaceRequirements.MUST
        left.elementType === XmlTokenType.XML_NAME && right.elementType === XmlTokenType.XML_NAME -> SpaceRequirements.MUST
        left.elementType === XmlTokenType.XML_TAG_NAME && right.elementType === XmlTokenType.XML_NAME -> SpaceRequirements.MUST
        else -> SpaceRequirements.MAY
    }

    override fun createElement(node: ASTNode): PsiElement {
        throw IllegalArgumentException("Unknown element: $node")
    }
}

class TmxFile internal constructor(viewProvider: FileViewProvider?) :
    XmlFileImpl(viewProvider, TMX_FILE_ELEMENT_TYPE), XmlFile {

    override fun toString(): String {
        return "TmxFile:" + this::class.simpleName
    }

    companion object {
        val TMX_FILE_ELEMENT_TYPE = IFileElementType("TMX_FILE_ELEMENT_TYPE", TMXLanguage.INSTANCE)
    }
}
