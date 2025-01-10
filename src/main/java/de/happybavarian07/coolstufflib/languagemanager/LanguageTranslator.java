package de.happybavarian07.coolstufflib.languagemanager;/*
 * @Author HappyBavarian07
 * @Date 20.07.2024 | 12:15
 */

public class LanguageTranslator {
    // This class is used to translate the language files into the desired language.
    // It should do the following (each step should be a method):
    // 1. Load language file
    // 2. Translate the provided language file or English as default into the desired language
    // 2.1. If the language file is not provided, use the English language file as default
    // 2.2. Go over each line, and if it is a string, translate it. If it is not a string, copy it as is into the new language file.
    // 2.2.1 If the line is a string, use the Google Translate API to translate it.
    // 2.2.2 If the line isn't a string, copy it as is into the new language file.
    // 2.2.3 If the line is a comment, copy it as is into the new language file.
    // 3. Save the translated language file.
    private LanguageFile originalLanguageFile;
    private LanguageFile translatedLanguageFile;
    private String desiredLanguage;
    private String originalLanguage;
    private String GOOGLE_TRANSLATE_API_KEY;
    // TODO: Implement later
    // Maybe use TensorFlow and a Pre-Trained Transformer Language Model https://www.tensorflow.org/text/tutorials/transformer
}
