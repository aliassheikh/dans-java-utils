package nl.knaw.dans.lib.util;

public class CheckDigit {

    public static boolean validateMod11Two(String str) {
        if (str.length() != 16) {
            return false;
        }

        char actual = str.charAt(str.length() - 1);
        int sum = new StringBuilder(str).substring(0, str.length() - 1)
                .chars()
                // convert digit character to numerical value
                .map(c -> c - 48)
                // add the result to the previous result and multiply it by 2
                .reduce(0, (i1, i2) -> (i1 + i2) * 2);

        // apply this calculation to the total
        int check = (12 - (sum % 11)) % 11;

        // convert numerical value back to character
        char expected = check == 10 ? 'X' : (char) (check + 48);

        return expected == actual;
    }
}
