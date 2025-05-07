package com.ugarosa.neovim.config.idea

import com.intellij.openapi.options.Configurable
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import com.ugarosa.neovim.common.getKeymapSettings
import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation
import com.ugarosa.neovim.mode.NeovimModeKind
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.AbstractCellEditor
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class NeovimKeymapConfigurable : Configurable {
    private val panel = JPanel(BorderLayout())
    private val tableModel: ListTableModel<Row>
    private val table: TableView<Row>
    private val settings = getKeymapSettings()

    init {
        // Define columns: Modes (multiple selection), LHS, RHS
        val modesColumn =
            object : ColumnInfo<Row, List<NeovimModeKind>>("Modes") {
                override fun valueOf(row: Row) = row.modes

                override fun isCellEditable(row: Row) = true

                override fun setValue(
                    row: Row,
                    value: List<NeovimModeKind>,
                ) {
                    row.modes = value
                }

                override fun getEditor(row: Row): TableCellEditor = MultiSelectCellEditor()

                override fun getRenderer(row: Row): TableCellRenderer = MultiSelectCellRenderer()
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
                    tableModel.addRow(Row(listOf(NeovimModeKind.NORMAL), "", ""))
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
                    ukm.modes,
                    ukm.lhs.join(),
                    ukm.rhs.join(),
                )
            }
        tableModel.items = rows
    }

    override fun disposeUIResources() {}

    private fun toUserKeyMappings(): List<UserKeyMapping> =
        tableModel.items.map { row ->
            UserKeyMapping(
                modes = row.modes,
                lhs = NeovimKeyNotation.parseNotations(row.lhs),
                rhs = KeyMappingAction.parseNotations(row.rhs),
            )
        }

    private data class Row(
        var modes: List<NeovimModeKind>,
        var lhs: String,
        var rhs: String,
    )

    // Cell Editor: Show dialog on button click
    private class MultiSelectCellEditor : AbstractCellEditor(), TableCellEditor {
        private var selected: List<NeovimModeKind> = emptyList()
        private val button =
            JButton().apply {
                addActionListener {
                    val panel =
                        JPanel().apply {
                            layout = BoxLayout(this, BoxLayout.Y_AXIS)
                            NeovimModeKind.entries.forEach { mode ->
                                val cb = JCheckBox(mode.name, selected.contains(mode))
                                cb.addActionListener { e ->
                                    selected =
                                        if (cb.isSelected) {
                                            selected + mode
                                        } else {
                                            selected - mode
                                        }
                                }
                                add(cb)
                            }
                        }
                    JOptionPane.showMessageDialog(
                        null,
                        panel,
                        "Select Modes",
                        JOptionPane.PLAIN_MESSAGE,
                    )
                    fireEditingStopped()
                }
            }

        override fun getTableCellEditorComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            row: Int,
            column: Int,
        ): Component {
            selected = (value as? List<*>)?.filterIsInstance<NeovimModeKind>() ?: emptyList()
            button.text = selected.joinToString(", ")
            return button
        }

        override fun getCellEditorValue(): Any = selected
    }

    // Cell Renderer: Show selected entries
    private class MultiSelectCellRenderer : JLabel(), TableCellRenderer {
        init {
            isOpaque = true
        }

        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int,
        ): Component {
            val modes = (value as? List<*>)?.filterIsInstance<NeovimModeKind>() ?: emptyList()
            text = modes.joinToString(", ")
            background = if (isSelected) table.selectionBackground else table.background
            return this
        }
    }
}
