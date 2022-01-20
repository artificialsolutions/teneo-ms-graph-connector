/**
 * This is an empty class in which to implement the methods necessary for the Teneo Engine to output messages in Microsoft Teams.
 */
class GraphTeamsUtils {
    static public String output(def input) {
        return new groovy.json.JsonBuilder(input).toString()
    }
}
