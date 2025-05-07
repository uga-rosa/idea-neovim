package com.ugarosa.neovim.config.idea

import com.intellij.openapi.options.Configurable
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import com.ugarosa.neovim.common.getKeymapSettings
import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class NeovimKeymapConfigurable : Configurable {
    private val panel = JPanel(BorderLayout())
    private val tableModel: ListTableModel<Row>
    private val table: TableView<Row>
    private val settings = getKeymapSettings()

    init {
        // Define columns: Mode, LHS, RHS
        val modesColumn =
            object : ColumnInfo<Row, String>("Mode") {
                override fun valueOf(row: Row) = row.mode

                override fun isCellEditable(row: Row) = true

                override fun setValue(
                    row: Row,
                    value: String,
                ) {
                    row.mode = value
                }
            }
        val lhsColumn =
            object : ColumnInfo<Row, String>("LHS") {
                override fun valueOf(row: Row) = row.lhs

                override fun isCellEditable(row: Row) = true

                override fun setValue(
                    row: Row,
                    value: String,
                ) {
                    row.lhs = value
                }
            }
        val rhsColumn =
            object : ColumnInfo<Row, String>("RHS") {
                override fun valueOf(row: Row) = row.rhs

                override fun isCellEditable(row: Row) = true

                override fun setValue(
                    row: Row,
                    value: String,
                ) {
                    row.rhs = value
                }
            }

        tableModel = ListTableModel(modesColumn, lhsColumn, rhsColumn)
        table = TableView(tableModel).apply { rowHeight = 25 }

        // Add/remove buttons
        val decorator =
            ToolbarDecorator.createDecorator(table)
                .setAddAction {
                    tableModel.addRow(Row("n", "", ""))
                }
                .setRemoveAction {
                    table.selectedRows.sortedDescending().forEach { tableModel.removeRow(it) }
                }
        panel.add(decorator.createPanel(), BorderLayout.CENTER)
    }

    override fun getDisplayName() = "Neovim Keymap"

    override fun createComponent(): JComponent {
        reset()
        return panel
    }

    override fun isModified(): Boolean {
        return settings.getUserKeyMappings() != toUserKeyMappings()
    }

    override fun apply() {
        settings.setUserKeyMappings(toUserKeyMappings())
    }

    override fun reset() {
        val rows =
            settings.getUserKeyMappings().map { ukm ->
                Row(
                    ukm.mode.value,
                    ukm.lhs.joinToString("") { it.toString() },
                    ukm.rhs.joinToString("") { it.toString() },
                )
            }
        tableModel.items = rows
    }

    override fun disposeUIResources() {}

    private fun toUserKeyMappings(): List<UserKeyMapping> =
        tableModel.items.map { row ->
            UserKeyMapping(
                mode = MapMode(row.mode),
                lhs = NeovimKeyNotation.parseNotations(row.lhs),
                rhs = KeyMappingAction.parseNotations(row.rhs),
            )
        }

    private data class Row(
        var mode: String,
        var lhs: String,
        var rhs: String,
    )
}
