import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // needed for [(ngModel)]
import { CostService } from '../../services/cost.service';
import { Cost } from '../../models/cost.model';

@Component({
  selector: 'app-cost',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cost.component.html',
  styleUrl: './cost.component.css'
})

export class CostComponent {
  private costService = inject(CostService);

  @Input() budgetId!: string;
  @Output() costAdded = new EventEmitter<void>();

  errorMessage = '';

  /** Inserted by Angular inject() migration for backwards compatibility */


  addNewCost(name: string, amount: number, ): void {
    if (!this.budgetId) {
      this.errorMessage = 'Keine Budget-ID gefunden!';
      return;
    }

    if (!amount || amount <= 0 || !name.trim() || amount < 0) {
      this.errorMessage = 'Bitte Name und gültigen Betrag angeben.';
      return;
    }


    const newCost: Partial<Cost> = {
      name: name,
      amount: amount
    };

    this.costService.addCostToBudget(this.budgetId, newCost as Cost).subscribe({
      next: (savedCost) => {
        console.log('Ausgabe erfolgreich hinzugefügt', savedCost);
      },
      error: (err) => {
        this.errorMessage = 'Fehler beim Speichern der Ausgabe.';
        console.error(err);
      }
    });
  }

  removeCost(costId: string | undefined): void {
    if (!costId) return;

    this.costService.deleteCost(costId).subscribe({
      next: () => {
        console.log('Ausgabe gelöscht');

        this.costAdded.emit();
      },
      error: () => {
        this.errorMessage = 'Fehler beim Löschen.';
      }
    });
  }
}
