import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { BudgetService } from './budget.service';
import { Budget } from '../../models/budget.model';

describe('BudgetService', () => {
  let service: BudgetService;
  let httpMock: HttpTestingController;

  const API_URL = 'http://localhost:8080/api/budgets';

  const mockBudget: Budget = {
    id: 'budget-1',
    name: 'Florenz Trip',
    amount: 1000,
    costs: [],
    travelEntry: {
      id: 'entry-1',
      startDate: '2025-06-01',
      endDate: '2025-06-14',
      color: '#FF5733'
    }
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        BudgetService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(BudgetService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // --- getAll ---

  it('getAll() should send GET to /api/budgets and return budgets', () => {
    service.getAll().subscribe(budgets => {
      expect(budgets.length).toBe(1);
      expect(budgets[0].name).toBe('Florenz Trip');
    });

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('GET');
    req.flush([mockBudget]);
  });

  // --- create ---

  it('create() should send POST to /api/budgets and return created budget', () => {
    const newBudget: Budget = {id: "", name: 'Tokyo Trip', amount: 2000 };

    service.create(newBudget).subscribe(budget => {
      expect(budget.id).toBe('budget-1');
      expect(budget.name).toBe('Florenz Trip');
    });

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newBudget);
    req.flush(mockBudget);
  });

  // --- update ---

  it('update() should send PUT to /api/budgets/:id and return updated budget', () => {
    const updatedBudget: Budget = { ...mockBudget, name: 'Florenz Updated', amount: 1500 };

    service.update('budget-1', updatedBudget).subscribe(budget => {
      expect(budget.name).toBe('Florenz Updated');
      expect(budget.amount).toBe(1500);
    });

    const req = httpMock.expectOne(`${API_URL}/budget-1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(updatedBudget);
    req.flush(updatedBudget);
  });

  // --- delete ---

  it('delete() should send DELETE to /api/budgets/:id', () => {
    service.delete('budget-1').subscribe(result => {
      expect(result).toBeNull(); // ← vorher toBeUndefined()
    });

    const req = httpMock.expectOne(`${API_URL}/budget-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
