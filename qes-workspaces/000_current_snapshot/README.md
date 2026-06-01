# QES current snapshot

Tato složka není fyzická kopie celého projektu. Je to čistý pracovní snapshot / konektor.

Důvod:
- nekopírovat celé repo znovu a znovu;
- udržet projekt čitelný;
- mít jasně uvedeno, z jakého commitu se vychází;
- pracovat přes menší protokoly a patch bloky;
- finální kód dát do source až po ověření.

Base branch: `qes-work-3-fix-rust-crate-name`
Base commit: `87315fd926129e641fc6fe359d769316f0b1a72d`

Pravidlo:
- source kód zůstává v původních složkách;
- pracovní návrhy a patch protokoly jsou v `qes-workspaces/`;
- každá fáze má vlastní podsložku;
- finální spojení se označí v `999_final/FINAL_CONNECTOR.md`.
