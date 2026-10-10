# The README's demo, recorded by docs/demo/record.sh with VHS: https://github.com/charmbracelet/vhs
# The story is pinned by ShishoBootstrapTest.theDemoProjectTellsTheDemoStory.
Output docs/demo/demo.gif

Set Shell bash
Set FontSize 15
Set Width 1180
Set Height 540
Set Padding 24
Set TypingSpeed 40ms

Hide
Type `shisho() { java -jar /repo/target/shisho.jar "$@"; }; rm -rf /tmp/loja && cp -r /repo/src/test/resources/fixtures/demo-loja /tmp/loja && cd /tmp && clear`
Enter
Show

Type "shisho analyze loja"
Sleep 500ms
Enter
Sleep 7s

Hide
Type "clear"
Enter
Show

Type "shisho fix --apply loja"
Sleep 500ms
Enter
Sleep 2s
Type "shisho analyze loja"
Sleep 500ms
Enter
Sleep 6s
