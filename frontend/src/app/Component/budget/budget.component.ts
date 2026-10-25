import { Component, OnInit, AfterViewInit, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Budget } from '../../models/budget.model';
import { BudgetService } from '../service/budget.service';
import { Cost } from '../../models/cost.model';
import { CostService } from '../../services/cost.service';
import { gsap } from 'gsap';

interface BudgetGroup {
  label: string;
  budgets: Budget[];
  isOpen: boolean;
}

interface MonthGroup {
  label: string;
  year: number;
  month: number;
  budgets: Budget[];
  isOpen: boolean;
}

interface SightseeingSuggestion {
  name: string;
  amount: number;
}

type DetailView = 'saved' | 'overbudget' | null;

@Component({
  selector: 'app-budget',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './budget.component.html',
  styleUrl: './budget.component.css'
})
export class BudgetComponent implements OnInit, AfterViewInit {

  @Input() tripId?: string;

  private budgetService = inject(BudgetService);
  private costService = inject(CostService);
  private http = inject(HttpClient);

  protected readonly Math = Math;

  budgets: Budget[] = [];
  isLoading = true;
  errorMessage = '';

  isModalOpen = false;
  newBudgetName = '';
  newBudgetAmount: number | null = null;
  editingBudget: Budget | null = null;

  expandedBudget: string | null = null;
  costsMap: Record<string, Cost[]> = {};
  showCostFormFor: string | null = null;
  costForm: Partial<Cost> = { name: '', amount: 0 };

  aiSuggestionsMap: Record<string, SightseeingSuggestion[] | null> = {};
  loadingSightsMap: Record<string, boolean> = {};
  scanningMap: Record<string, boolean> = {};

  tripGroups: BudgetGroup[] = [];
  monthGroups: MonthGroup[] = [];

  activeDetailView: DetailView = null;

  ngOnInit(): void {
    this.loadBudgets();
  }

  ngAfterViewInit(): void {
    this.runEnterAnimations();
  }

  private runEnterAnimations(): void {
    const tl = gsap.timeline({ defaults: { ease: 'power3.out' } });
    tl.fromTo('.budget-header', { opacity: 0, y: -30 }, { opacity: 1, y: 0, duration: 0.6 })
      .fromTo('.kpi-card',      { opacity: 0, y: 25  }, { opacity: 1, y: 0, duration: 0.45, stagger: 0.1 }, '-=0.3')
      .fromTo('.budget-list',   { opacity: 0, y: 20  }, { opacity: 1, y: 0, duration: 0.5 }, '-=0.2')
      .fromTo('.donut-section', { opacity: 0, x: 20  }, { opacity: 1, x: 0, duration: 0.5 }, '-=0.4');
  }

  // ── HELPERS ──────────────────────────────────────────────────

  private isPast(budget: Budget): boolean {
    if (!budget.travelEntry?.endDate) return false;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return new Date(budget.travelEntry.endDate) < today;
  }

  // ── KPI GETTERS ──────────────────────────────────────────────

  get totalBudget(): number {
    return this.budgets.reduce((s, b) => s + b.amount, 0);
  }

  get totalSpent(): number {
    return Math.round(this.budgets.reduce((s, b) => s + this.calculateTotal(b), 0) * 100) / 100;
  }

  get pastBudgets(): Budget[] {
    return this.budgets.filter(b => this.isPast(b));
  }

  get totalBalance(): number {
    return Math.round(this.pastBudgets.reduce((sum, b) => sum + (b.amount - this.calculateTotal(b)), 0) * 100) / 100;
  }

  get savedBudgets(): Budget[] {
    return this.pastBudgets.filter(b => this.calculateTotal(b) <= b.amount);
  }

  get overBudgetBudgets(): Budget[] {
    return this.budgets.filter(b => this.calculateTotal(b) > b.amount);
  }

  get overBudgetCount(): number {
    return this.overBudgetBudgets.length;
  }

  // ── KPI DETAIL VIEW ──────────────────────────────────────────

  openDetailView(view: DetailView): void {
    this.activeDetailView = view;
    setTimeout(() => {
      const el = document.querySelector('.detail-view');
      if (el) gsap.fromTo(el,
        { opacity: 0, y: 15 },
        { opacity: 1, y: 0, duration: 0.35, ease: 'power2.out' }
      );
    }, 10);
  }

  closeDetailView(): void {
    const el = document.querySelector('.detail-view');
    if (el) {
      gsap.to(el, {
        opacity: 0, y: 10, duration: 0.25, ease: 'power2.in',
        onComplete: () => { this.activeDetailView = null; }
      });
    } else {
      this.activeDetailView = null;
    }
  }

  // ── GROUPING ─────────────────────────────────────────────────

  private buildTripGroups(): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const past: Budget[]     = [];
    const active: Budget[]   = [];
    const upcoming: Budget[] = [];

    this.budgets.forEach(b => {
      if (!b.travelEntry) { upcoming.push(b); return; }
      const start = new Date(b.travelEntry.startDate);
      const end   = new Date(b.travelEntry.endDate);
      if (end < today)         past.push(b);
      else if (start <= today) active.push(b);
      else                     upcoming.push(b);
    });

    this.tripGroups = [
      { label: 'Active',   budgets: active,   isOpen: true  },
      { label: 'Upcoming', budgets: upcoming, isOpen: true  },
      { label: 'Past',     budgets: past,     isOpen: false },
    ].filter(g => g.budgets.length > 0);
  }

  private buildMonthGroups(): void {
    const map = new Map<string, { year: number; month: number; budgets: Budget[] }>();

    this.budgets.forEach(b => {
      if (!b.travelEntry) return;
      const d     = new Date(b.travelEntry.startDate);
      const year  = d.getFullYear();
      const month = d.getMonth();
      const key   = `${year}-${String(month + 1).padStart(2, '0')}`;
      if (!map.has(key)) map.set(key, { year, month, budgets: [] });
      map.get(key)!.budgets.push(b);
    });

    const today = new Date();
    this.monthGroups = Array.from(map.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([, v]) => ({
        label:   new Date(v.year, v.month, 1).toLocaleString('en', { month: 'long', year: 'numeric' }),
        year:    v.year,
        month:   v.month,
        budgets: v.budgets,
        isOpen:  v.year === today.getFullYear() && v.month === today.getMonth()
      }));
  }

  toggleTripGroup(group: BudgetGroup): void {
    group.isOpen = !group.isOpen;
  }

  toggleMonthGroup(group: MonthGroup): void {
    group.isOpen = !group.isOpen;
  }

  // ── MODAL ────────────────────────────────────────────────────

  toggleModal(): void {
    if (!this.isModalOpen) {
      this.isModalOpen = true;
      setTimeout(() => {
        gsap.fromTo('.modal-backdrop', { opacity: 0 }, { opacity: 1, duration: 0.25 });
        gsap.fromTo('.modal-box',
          { scale: 0.88, opacity: 0, y: 20 },
          { scale: 1, opacity: 1, y: 0, duration: 0.4, ease: 'back.out(1.4)' }
        );
      }, 10);
    } else {
      this.animateModalOut(() => {
        this.isModalOpen = false;
        this.newBudgetName = '';
        this.newBudgetAmount = null;
        this.editingBudget = null;
      });
    }
  }

  private animateModalOut(callback: () => void): void {
    const tl = gsap.timeline({ onComplete: callback });
    tl.to('.modal-box',      { scale: 0.9, opacity: 0, y: 15, duration: 0.25, ease: 'power2.in' })
      .to('.modal-backdrop', { opacity: 0, duration: 0.2 }, '-=0.1');
  }

  openBudgetForm(budget?: Budget): void {
    if (budget) {
      this.editingBudget = budget;
      this.newBudgetName = budget.name;
      this.newBudgetAmount = budget.amount;
    }
    this.toggleModal();
  }

  submitBudget(): void {
    if (!this.newBudgetName || !this.newBudgetAmount) return;

    if (this.editingBudget?.id) {
      const updated: Budget = { ...this.editingBudget, name: this.newBudgetName, amount: this.newBudgetAmount };
      this.budgetService.update(this.editingBudget.id, updated).subscribe({
        next: () => { this.loadBudgets(); this.toggleModal(); },
        error: () => this.errorMessage = 'Update failed.'
      });
    }
  }

  // ── BUDGETS ──────────────────────────────────────────────────

  loadBudgets(): void {
    this.isLoading = true;
    this.budgetService.getAll().subscribe({
      next: (data) => {
        this.budgets = this.tripId
          ? data.filter(b => String(b.travelEntry?.id) === String(this.tripId))
          : data;

        this.budgets.forEach(b => { if (b.id) this.costsMap[b.id] = b.costs || []; });
        this.buildTripGroups();
        this.buildMonthGroups();
        this.isLoading = false;
      },
      error: (err) => { console.error(err); this.isLoading = false; }
    });
  }

  deleteBudget(id: string | undefined): void {
    if (!id) return;
    if (confirm('Delete this budget?')) {
      gsap.to(`#budget-card-${id}`, {
        opacity: 0, scale: 0.85, x: -20, duration: 0.3, ease: 'power2.in',
        onComplete: () => {
          this.budgetService.delete(id).subscribe({
            next: () => {
              this.budgets = this.budgets.filter(b => b.id !== id);
              if (this.expandedBudget === id) this.expandedBudget = null;
              this.buildTripGroups();
              this.buildMonthGroups();
            },
            error: () => this.errorMessage = 'Delete failed.'
          });
        }
      });
    }
  }

  toggleExpand(budget: Budget): void {
    const id = budget.id!;
    if (this.expandedBudget === id) {
      const el = document.querySelector(`#expanded-${id}`);
      if (el) {
        gsap.to(el, {
          height: 0, opacity: 0, duration: 0.3, ease: 'power2.inOut',
          onComplete: () => { this.expandedBudget = null; }
        });
      } else {
        this.expandedBudget = null;
      }
    } else {
      this.expandedBudget = id;
      this.costsMap[id] = budget.costs || [];
      this.showCostFormFor = null;
      setTimeout(() => {
        const el = document.querySelector(`#expanded-${id}`);
        if (el) {
          gsap.fromTo(el,
            { height: 0, opacity: 0 },
            { height: 'auto', opacity: 1, duration: 0.35, ease: 'power2.out' }
          );
        }
      }, 10);
    }
  }

  // ── COSTS ────────────────────────────────────────────────────

  openCostForm(budgetId: string, cost?: Cost): void {
    this.showCostFormFor = budgetId;
    this.costForm = cost ? { ...cost } : { name: '', amount: 0 };
    setTimeout(() => {
      const el = document.querySelector('.cost-form');
      if (el) gsap.fromTo(el,
        { opacity: 0, y: -10 },
        { opacity: 1, y: 0, duration: 0.25, ease: 'power2.out' }
      );
    }, 10);
  }

  closeCostForm(): void {
    this.showCostFormFor = null;
    this.costForm = { name: '', amount: 0 };
  }

  saveCost(budget: Budget): void {
    const newCost = { name: this.costForm.name!, amount: this.costForm.amount!, budget };
    this.costService.addCostToBudget(budget.id!, newCost).subscribe({
      next: () => { this.closeCostForm(); this.loadBudgets(); },
      error: (err) => console.error(err)
    });
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  deleteCost(costId: string, _budgetId: string): void {
    if (confirm('Are you sure you want to delete this cost?')) {
      this.costService.deleteCost(costId).subscribe({
        next: () => this.loadBudgets(),
        error: (err) => console.error(err)
      });
    }
  }

  calculateTotal(budget: Budget): number {
    const costs = this.costsMap[budget.id!] || [];
    return Math.round(costs.reduce((acc, curr) => acc + curr.amount, 0) * 100) / 100;
  }

  onScanFileSelected(event: Event, budget: Budget): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;

    const budgetId = budget.id!;
    this.scanningMap[budgetId] = true;

    this.costService.scanCostDocument(budgetId, file).subscribe({
      next: () => {
        this.scanningMap[budgetId] = false;
        this.loadBudgets();
      },
      error: (err) => {
        this.scanningMap[budgetId] = false;
        alert(typeof err.error === 'string' && err.error ? err.error : 'Scanning the document failed.');
      }
    });
  }

  // ── AI SIGHTSEEING ───────────────────────────────────────────

  fetchSightseeingSuggestions(budget: Budget): void {
    const budgetId = budget.id!;
    this.loadingSightsMap[budgetId] = true;

    this.http.get<SightseeingSuggestion[]>(`http://localhost:8080/api/sightseeing/suggestions/${budgetId}`).subscribe({
      next: (res) => {
        this.aiSuggestionsMap[budgetId] = res;
        this.loadingSightsMap[budgetId] = false;
        setTimeout(() => {
          const el = document.querySelector(`#ai-suggestions-${budgetId}`);
          if (el) gsap.fromTo(el,
            { opacity: 0, y: 10 },
            { opacity: 1, y: 0, duration: 0.3, ease: 'power2.out' }
          );
        }, 10);
      },
      error: (err) => {
        console.error(err);
        alert('Error loading AI suggestions!');
        this.loadingSightsMap[budgetId] = false;
      }
    });
  }

  addAiSightToBudget(budget: Budget, sight: SightseeingSuggestion): void {
    const newCost = { name: sight.name, amount: sight.amount, budget };
    this.costService.addCostToBudget(budget.id!, newCost).subscribe({
      next: () => {
        if (this.aiSuggestionsMap[budget.id!]) {
          this.aiSuggestionsMap[budget.id!] = this.aiSuggestionsMap[budget.id!]!.filter(s => s.name !== sight.name);
        }
        this.loadBudgets();
      },
      error: (err) => console.error(err)
    });
  }
}
